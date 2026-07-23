package com.mingzhe.resumetailor.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingzhe.resumetailor.openai.OpenAiResumeResponse;
import com.mingzhe.resumetailor.openai.OpenAiResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeServiceRetryTest {

    @Test
    void retriesWhenTextValidationPassesButJsonParsingFails() {
        OpenAiResumeService openAiResumeService = mock(OpenAiResumeService.class);
        OpenAiResumeResponse malformed = response(
                """
                {"experience":"Backend engineering experience with production systems and APIs","projects":[],"skills":["Java"],"summary":"Long enough to pass the existing text checks","sections":[{} }]}
                """
        );
        OpenAiResumeResponse valid = response(
                """
                {
                  "experience": "Backend engineering experience with production systems and APIs",
                  "projects": [],
                  "skills": ["Java"],
                  "summary": "Long enough to pass the existing text checks"
                }
                """
        );
        when(openAiResumeService.generateWithUsage("prompt")).thenReturn(malformed, valid);
        ResumeService resumeService = createService(openAiResumeService);

        OpenAiResumeResponse result =
                ReflectionTestUtils.invokeMethod(resumeService, "callLlmWithRetry", "prompt");

        assertEquals(
                "{\"experience\":\"Backend engineering experience with production systems and APIs\","
                        + "\"projects\":[],\"skills\":[\"Java\"],"
                        + "\"summary\":\"Long enough to pass the existing text checks\"}",
                result.getContent()
        );
        verify(openAiResumeService, times(2)).generateWithUsage("prompt");
    }

    @Test
    void stopsAfterThreeJsonParsingFailures() {
        OpenAiResumeService openAiResumeService = mock(OpenAiResumeService.class);
        OpenAiResumeResponse malformed = response(
                """
                {"experience":"Backend engineering experience with production systems and APIs","projects":[],"skills":["Java"],"summary":"Long enough to pass the existing text checks","sections":[{} }]}
                """
        );
        when(openAiResumeService.generateWithUsage("prompt")).thenReturn(malformed);
        ResumeService resumeService = createService(openAiResumeService);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(resumeService, "callLlmWithRetry", "prompt")
        );

        assertEquals("Resume generation failed after 3 attempts", exception.getMessage());
        verify(openAiResumeService, times(3)).generateWithUsage("prompt");
    }

    private OpenAiResumeResponse response(String content) {
        OpenAiResumeResponse response = new OpenAiResumeResponse();
        response.setContent(content);
        return response;
    }

    private ResumeService createService(OpenAiResumeService openAiResumeService) {
        return new ResumeService(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new ObjectMapper(),
                openAiResumeService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                3
        );
    }
}

package com.mingzhe.resumetailor.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mingzhe.resumetailor.education.Education;
import com.mingzhe.resumetailor.experience.Experience;
import com.mingzhe.resumetailor.openai.OpenAiResumeResponse;
import com.mingzhe.resumetailor.openai.OpenAiResumeService;
import com.mingzhe.resumetailor.project.Project;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeServiceRetryTest {

    @Test
    void formatsNormalPromptDatesLikeRagContextDates() {
        ResumeService resumeService = createService(mock(OpenAiResumeService.class));

        Experience experience = new Experience();
        experience.setStartDate(LocalDate.of(2025, 6, 23));
        experience.setEndDate(LocalDate.of(2025, 9, 12));

        Education education = new Education();
        education.setStartDate(LocalDate.of(2021, 9, 15));
        education.setEndDate(LocalDate.of(2025, 6, 20));

        Project project = new Project();
        project.setStartDate(LocalDate.of(2026, 4, 1));
        project.setEndDate(null);

        String experiencePrompt =
                ReflectionTestUtils.invokeMethod(resumeService, "buildExperiences", List.of(experience));
        String educationPrompt =
                ReflectionTestUtils.invokeMethod(resumeService, "buildEducations", List.of(education));
        String projectPrompt =
                ReflectionTestUtils.invokeMethod(resumeService, "buildProjects", List.of(project));

        assertTrue(experiencePrompt.contains("Start Date: Jun 2025"));
        assertTrue(experiencePrompt.contains("End Date: Sep 2025"));
        assertFalse(experiencePrompt.contains("2025-06-23"));
        assertTrue(educationPrompt.contains("Start Date: Sep 2021"));
        assertTrue(educationPrompt.contains("End Date: Jun 2025"));
        assertTrue(projectPrompt.contains("Start Date: Apr 2026"));
        assertTrue(projectPrompt.contains("End Date: Present"));
    }

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

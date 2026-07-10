package com.mingzhe.resumetailor.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Calls the OpenAI chat completion API to generate resume content.
 */
@Service
public class OpenAiResumeService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiResumeService.class);

    // Keep the API key outside application config files and Docker images.
    private final String apiKey = System.getenv("OPENAI_API_KEY");
    private static final String DEFAULT_MODEL_NAME = "gpt-5.6-terra";
    private final String modelName = getEnvironmentValue("OPENAI_MODEL_NAME", DEFAULT_MODEL_NAME);

    public String getModelName() {
        return modelName;
    }

    public String generate(String prompt) {
        return generateWithUsage(prompt).getContent();
    }

    public OpenAiResumeResponse generateWithUsage(String prompt) {
        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String safePrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t");

            String body = """
            {
              "model": "%s",
              "messages": [
                {"role": "user", "content": "%s"}
              ]
            }
            """.formatted(modelName, safePrompt);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();

            InputStream inputStream;

            if (statusCode >= 200 && statusCode < 300) {
                inputStream = conn.getInputStream();
            } else {
                inputStream = conn.getErrorStream();
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            String responseBody = response.toString();

            System.out.println("OpenAI status code: " + statusCode);
            System.out.println("OpenAI response: " + responseBody);

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("OpenAI API call failed with status code: " + statusCode);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(responseBody);

            OpenAiResumeResponse resumeResponse = new OpenAiResumeResponse();
            resumeResponse.setContent(root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText());

            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode() && !usage.isNull()) {
                // Usage fields are optional across model/API variants; when
                // present they feed generation_history token/cost tracking.
                resumeResponse.setInputTokenCount(readOptionalInt(usage, "prompt_tokens", "input_tokens"));
                resumeResponse.setOutputTokenCount(readOptionalInt(usage, "completion_tokens", "output_tokens"));
            }

            return resumeResponse;

        } catch (Exception e) {
            log.error("OpenAI resume generation call failed", e);
            OpenAiResumeResponse response = new OpenAiResumeResponse();
            response.setContent("AI call failed");
            return response;
        }
    }

    private Integer readOptionalInt(JsonNode node, String primaryField, String fallbackField) {
        JsonNode value = node.path(primaryField);
        if (value.isMissingNode() || value.isNull()) {
            value = node.path(fallbackField);
        }

        return value.isInt() || value.isLong() ? value.asInt() : null;
    }

    private String getEnvironmentValue(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}

package com.mingzhe.resumetailor.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

            String body = buildRequestBody(prompt);

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

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);

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

    String buildRequestBody(String prompt) throws JsonProcessingException {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("model", modelName);
        body.put("reasoning_effort", "low");
        body.put("max_completion_tokens", 3072);

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        ObjectNode responseFormat = body.putObject("response_format");
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", "tailored_resume");
        jsonSchema.put(
                "description",
                "A tailored ATS resume with contact information, an optional visible summary, and selected resume sections."
        );
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", ResumeOutputJsonSchema.create(OBJECT_MAPPER));

        return OBJECT_MAPPER.writeValueAsString(body);
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

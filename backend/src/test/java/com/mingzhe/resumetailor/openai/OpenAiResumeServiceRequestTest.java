package com.mingzhe.resumetailor.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiResumeServiceRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsStrictResumeJsonSchemaRequest() throws Exception {
        OpenAiResumeService service = new OpenAiResumeService();
        String prompt = "Generate a \"tailored\" resume.\nUse only supported facts.";

        JsonNode body = objectMapper.readTree(service.buildRequestBody(prompt));

        assertEquals("low", body.path("reasoning_effort").asText());
        assertEquals(3072, body.path("max_completion_tokens").asInt());
        assertEquals(prompt, body.path("messages").get(0).path("content").asText());

        JsonNode responseFormat = body.path("response_format");
        assertEquals("json_schema", responseFormat.path("type").asText());
        assertEquals("tailored_resume", responseFormat.path("json_schema").path("name").asText());
        assertTrue(responseFormat.path("json_schema").path("strict").asBoolean());

        JsonNode schema = responseFormat.path("json_schema").path("schema");
        assertEquals("object", schema.path("type").asText());
        assertEquals("ATS", schema.path("properties").path("template").path("enum").get(0).asText());
        assertEquals(4, schema.path("properties").path("sections").path("items").path("anyOf").size());
        assertStrictObjectSchemas(schema);
    }

    private void assertStrictObjectSchemas(JsonNode node) {
        if (node.isObject() && "object".equals(node.path("type").asText())) {
            assertTrue(node.has("additionalProperties"));
            assertFalse(node.path("additionalProperties").asBoolean());

            Set<String> propertyNames = new HashSet<>();
            node.path("properties").fieldNames().forEachRemaining(propertyNames::add);

            Set<String> requiredNames = new HashSet<>();
            node.path("required").forEach(required -> requiredNames.add(required.asText()));
            assertEquals(propertyNames, requiredNames);
        }

        if (node.isContainerNode()) {
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                assertStrictObjectSchemas(children.next());
            }
        }
    }
}

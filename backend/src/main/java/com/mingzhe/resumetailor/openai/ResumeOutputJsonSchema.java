package com.mingzhe.resumetailor.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class ResumeOutputJsonSchema {

    private ResumeOutputJsonSchema() {
    }

    static ObjectNode create(ObjectMapper objectMapper) {
        ObjectNode root = objectSchema(
                objectMapper,
                "template", enumStringSchema(objectMapper, "ATS"),
                "contact", contactSchema(objectMapper),
                "summary", summarySchema(objectMapper),
                "sections", arraySchema(objectMapper, sectionSchema(objectMapper))
        );

        ObjectNode definitions = objectMapper.createObjectNode();
        definitions.set("education_section", educationSectionSchema(objectMapper));
        definitions.set("experience_section", experienceSectionSchema(objectMapper));
        definitions.set("project_section", projectSectionSchema(objectMapper));
        definitions.set("skill_section", skillSectionSchema(objectMapper));
        root.set("$defs", definitions);

        return root;
    }

    private static ObjectNode contactSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "name", stringSchema(objectMapper),
                "location", stringSchema(objectMapper),
                "email", stringSchema(objectMapper),
                "phone", stringSchema(objectMapper),
                "linkedin", stringSchema(objectMapper),
                "github", stringSchema(objectMapper)
        );
    }

    private static ObjectNode summarySchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "visible", booleanSchema(objectMapper),
                "content", stringSchema(objectMapper)
        );
    }

    private static ObjectNode sectionSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        ArrayNode anyOf = schema.putArray("anyOf");
        anyOf.add(refSchema(objectMapper, "#/$defs/education_section"));
        anyOf.add(refSchema(objectMapper, "#/$defs/experience_section"));
        anyOf.add(refSchema(objectMapper, "#/$defs/project_section"));
        anyOf.add(refSchema(objectMapper, "#/$defs/skill_section"));
        return schema;
    }

    private static ObjectNode educationSectionSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "id", enumStringSchema(objectMapper, "education"),
                "type", enumStringSchema(objectMapper, "education"),
                "title", stringSchema(objectMapper),
                "visible", booleanSchema(objectMapper),
                "order", integerSchema(objectMapper),
                "items", arraySchema(objectMapper, educationItemSchema(objectMapper))
        );
    }

    private static ObjectNode educationItemSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "school", stringSchema(objectMapper),
                "degree", stringSchema(objectMapper),
                "major", stringSchema(objectMapper),
                "location", stringSchema(objectMapper),
                "startDate", stringSchema(objectMapper),
                "endDate", stringSchema(objectMapper),
                "gpa", stringSchema(objectMapper),
                "details", stringArraySchema(objectMapper)
        );
    }

    private static ObjectNode experienceSectionSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "id", enumStringSchema(objectMapper, "experience"),
                "type", enumStringSchema(objectMapper, "experience"),
                "title", stringSchema(objectMapper),
                "visible", booleanSchema(objectMapper),
                "order", integerSchema(objectMapper),
                "items", arraySchema(objectMapper, experienceItemSchema(objectMapper))
        );
    }

    private static ObjectNode experienceItemSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "company", stringSchema(objectMapper),
                "title", stringSchema(objectMapper),
                "location", stringSchema(objectMapper),
                "startDate", stringSchema(objectMapper),
                "endDate", stringSchema(objectMapper),
                "visible", booleanSchema(objectMapper),
                "bullets", stringArraySchema(objectMapper)
        );
    }

    private static ObjectNode projectSectionSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "id", enumStringSchema(objectMapper, "projects"),
                "type", enumStringSchema(objectMapper, "projects"),
                "title", stringSchema(objectMapper),
                "visible", booleanSchema(objectMapper),
                "order", integerSchema(objectMapper),
                "items", arraySchema(objectMapper, projectItemSchema(objectMapper))
        );
    }

    private static ObjectNode projectItemSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "name", stringSchema(objectMapper),
                "techStack", stringArraySchema(objectMapper),
                "startDate", stringSchema(objectMapper),
                "endDate", stringSchema(objectMapper),
                "visible", booleanSchema(objectMapper),
                "bullets", stringArraySchema(objectMapper)
        );
    }

    private static ObjectNode skillSectionSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "id", enumStringSchema(objectMapper, "skills"),
                "type", enumStringSchema(objectMapper, "skills"),
                "title", stringSchema(objectMapper),
                "visible", booleanSchema(objectMapper),
                "order", integerSchema(objectMapper),
                "items", arraySchema(objectMapper, skillItemSchema(objectMapper))
        );
    }

    private static ObjectNode skillItemSchema(ObjectMapper objectMapper) {
        return objectSchema(
                objectMapper,
                "category", stringSchema(objectMapper),
                "skills", stringArraySchema(objectMapper)
        );
    }

    private static ObjectNode objectSchema(ObjectMapper objectMapper, Object... propertyPairs) {
        if (propertyPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Object schema properties must be name/schema pairs.");
        }

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        for (int index = 0; index < propertyPairs.length; index += 2) {
            String name = (String) propertyPairs[index];
            JsonNode propertySchema = (JsonNode) propertyPairs[index + 1];
            properties.set(name, propertySchema);
            required.add(name);
        }

        schema.put("additionalProperties", false);
        return schema;
    }

    private static ObjectNode arraySchema(ObjectMapper objectMapper, JsonNode itemSchema) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "array");
        schema.set("items", itemSchema);
        return schema;
    }

    private static ObjectNode stringArraySchema(ObjectMapper objectMapper) {
        return arraySchema(objectMapper, stringSchema(objectMapper));
    }

    private static ObjectNode stringSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "string");
        return schema;
    }

    private static ObjectNode enumStringSchema(ObjectMapper objectMapper, String value) {
        ObjectNode schema = stringSchema(objectMapper);
        schema.putArray("enum").add(value);
        return schema;
    }

    private static ObjectNode booleanSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "boolean");
        return schema;
    }

    private static ObjectNode integerSchema(ObjectMapper objectMapper) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "integer");
        return schema;
    }

    private static ObjectNode refSchema(ObjectMapper objectMapper, String reference) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$ref", reference);
        return schema;
    }
}

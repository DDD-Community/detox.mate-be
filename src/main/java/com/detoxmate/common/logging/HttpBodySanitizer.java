package com.detoxmate.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class HttpBodySanitizer {

    static final int MAX_BODY_LENGTH = 2048;
    static final int MAX_STRING_FIELD_LENGTH = 200;
    private static final String MASKED_VALUE = "***";
    private static final String TRUNCATED_SUFFIX = "...[truncated]";
    private static final String UNPARSEABLE_JSON = "[unparseable-json]";

    private final ObjectMapper objectMapper;

    public HttpBodySanitizer() {
        this(new ObjectMapper());
    }

    HttpBodySanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String sanitize(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode sanitized = sanitizeNode(root);
            return truncateBody(objectMapper.writeValueAsString(sanitized));
        } catch (Exception exception) {
            return UNPARSEABLE_JSON;
        }
    }

    private JsonNode sanitizeNode(JsonNode node) {
        if (node == null) {
            return null;
        }

        if (node.isObject()) {
            ObjectNode objectNode = ((ObjectNode) node).deepCopy();
            Iterator<String> fieldNames = objectNode.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (SensitiveLogKeywordMatcher.matches(fieldName)) {
                    objectNode.put(fieldName, MASKED_VALUE);
                    continue;
                }

                objectNode.set(fieldName, sanitizeNode(objectNode.get(fieldName)));
            }

            return objectNode;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                arrayNode.add(sanitizeNode(item));
            }
            return arrayNode;
        }

        if (node.isTextual()) {
            String value = node.asText();
            if (value.length() > MAX_STRING_FIELD_LENGTH) {
                return TextNode.valueOf(value.substring(0, MAX_STRING_FIELD_LENGTH) + TRUNCATED_SUFFIX);
            }
        }

        return node;
    }

    private String truncateBody(String body) {
        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }

        return body.substring(0, MAX_BODY_LENGTH) + TRUNCATED_SUFFIX;
    }
}

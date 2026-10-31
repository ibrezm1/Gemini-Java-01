package com.example.gemini.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonMapperUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonMapperUtil() {
        // utility class
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    /**
     * Parses a raw JSON string into a strongly-typed Java class (such as a Record).
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        // Clean markdown code fence if Gemini returned ```json ... ```
        String sanitized = cleanMarkdownCodeBlock(json);
        return MAPPER.readValue(sanitized, clazz);
    }

    /**
     * Formats any Java object into an indented, pretty-printed JSON string.
     */
    public static String toPrettyJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    /**
     * Prettifies a raw JSON string.
     */
    public static String formatJson(String rawJson) {
        try {
            String sanitized = cleanMarkdownCodeBlock(rawJson);
            Object tree = MAPPER.readValue(sanitized, Object.class);
            return MAPPER.writeValueAsString(tree);
        } catch (Exception e) {
            return rawJson;
        }
    }

    /**
     * Strips Markdown code blocks like ```json ... ``` if present.
     */
    public static String cleanMarkdownCodeBlock(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}


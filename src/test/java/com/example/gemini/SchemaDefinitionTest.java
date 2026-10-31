package com.example.gemini;

import com.example.gemini.service.ContentModerationService;
import com.example.gemini.service.MovieAnalysisService;
import com.example.gemini.service.RecipeExtractorService;
import com.google.genai.types.Schema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchemaDefinitionTest {

    @Test
    @DisplayName("Recipe schema must define required properties")
    void testRecipeSchema() {
        Map<String, Object> schema = RecipeExtractorService.createRecipeSchema();

        assertEquals("object", schema.get("type"));
        assertTrue(schema.containsKey("properties"));
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertTrue(required.contains("recipe_name"));
        assertTrue(required.contains("ingredients"));
        assertTrue(required.contains("instructions"));
    }

    @Test
    @DisplayName("Content Moderation schema must include anyOf decision")
    void testModerationSchema() {
        Map<String, Object> schema = ContentModerationService.createModerationSchema();

        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> decision = (Map<String, Object>) properties.get("decision");
        assertTrue(decision.containsKey("anyOf"));
    }

    @Test
    @DisplayName("Movie schema must define SDK Schema object")
    void testMovieSchema() {
        Schema schema = MovieAnalysisService.createMovieSchema();

        assertNotNull(schema);
        assertEquals("OBJECT", schema.type().map(Object::toString).orElse(""));
        assertTrue(schema.properties().isPresent());
        Map<String, Schema> props = schema.properties().get();
        assertTrue(props.containsKey("title"));
        assertTrue(props.containsKey("sentiment"));
        assertTrue(props.containsKey("rating_out_of_10"));
        assertTrue(props.containsKey("key_themes"));
    }
}

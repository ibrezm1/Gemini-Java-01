package com.example.gemini;

import com.example.gemini.model.ModerationResult;
import com.example.gemini.model.MovieAnalysis;
import com.example.gemini.model.Recipe;
import com.example.gemini.model.Sentiment;
import com.example.gemini.service.ContentModerationService;
import com.example.gemini.service.MovieAnalysisService;
import com.example.gemini.service.RecipeExtractorService;
import com.example.gemini.util.JsonMapperUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonMapperUtilTest {

    @Test
    @DisplayName("Should parse Recipe JSON into Recipe record")
    void testParseRecipe() {
        Recipe recipe = RecipeExtractorService.getMockRecipe();

        assertNotNull(recipe);
        assertEquals("Delicious Chocolate Chip Cookies", recipe.recipeName());
        assertEquals(15, recipe.prepTimeMinutes());
        assertEquals(9, recipe.ingredients().size());
        assertEquals("all-purpose flour", recipe.ingredients().get(0).name());
        assertEquals("2 and 1/4 cups", recipe.ingredients().get(0).quantity());
        assertEquals(7, recipe.instructions().size());
    }

    @Test
    @DisplayName("Should parse Spam Moderation JSON")
    void testParseSpamModeration() {
        ModerationResult result = ContentModerationService.getMockModeration(true);

        assertNotNull(result);
        assertNotNull(result.decision());
        assertTrue(result.decision().isSpam());
        assertEquals("scam", result.decision().spamType());
        assertNotNull(result.decision().reason());
        assertNull(result.decision().isSafe());
    }

    @Test
    @DisplayName("Should parse Safe Content Moderation JSON")
    void testParseSafeModeration() {
        ModerationResult result = ContentModerationService.getMockModeration(false);

        assertNotNull(result);
        assertNotNull(result.decision());
        assertFalse(result.decision().isSpam());
        assertTrue(result.decision().isSafe());
        assertNotNull(result.decision().summary());
    }

    @Test
    @DisplayName("Should parse Movie Analysis JSON with Sentiment enum")
    void testParseMovieAnalysis() {
        MovieAnalysis analysis = MovieAnalysisService.getMockAnalysis();

        assertNotNull(analysis);
        assertEquals("Inception", analysis.title());
        assertEquals(2010, analysis.releaseYear());
        assertEquals(Sentiment.POSITIVE, analysis.sentiment());
        assertEquals(9.3, analysis.ratingOutOf10());
        assertTrue(analysis.keyThemes().contains("Dreams vs. Reality"));
    }

    @Test
    @DisplayName("Should clean Markdown code fences from JSON string")
    void testCleanMarkdownCodeFence() throws Exception {
        String fenced = "```json\n{\"recipe_name\": \"Pancakes\", \"ingredients\": [], \"instructions\": []}\n```";
        Recipe recipe = JsonMapperUtil.fromJson(fenced, Recipe.class);

        assertNotNull(recipe);
        assertEquals("Pancakes", recipe.recipeName());
    }
}


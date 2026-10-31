package com.example.gemini.service;

import com.example.gemini.model.Recipe;
import com.example.gemini.util.JsonMapperUtil;
import com.google.genai.Client;
import com.google.genai.gaos.models.interactions.CreateModelInteraction;
import com.google.genai.gaos.models.interactions.CreateModelInteractionResponseFormat;
import com.google.genai.gaos.models.interactions.Interaction;
import com.google.genai.gaos.models.interactions.InteractionsInput;
import com.google.genai.gaos.models.interactions.Model;
import com.google.genai.gaos.models.interactions.ResponseFormat;
import com.google.genai.gaos.models.interactions.TextResponseFormat;
import com.google.genai.gaos.models.interactions.TextResponseFormatMimeType;
import com.google.genai.gaos.models.operations.CreateInteractionRequestBody;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates Structured Output extraction using the official Gemini Interactions API.
 * Maps unstructured text into a strongly typed Recipe Java record.
 */
public class RecipeExtractorService {

    public static final String DEFAULT_RECIPE_TEXT =
            """
            Please extract the recipe from the following text:
            The user wants to make delicious chocolate chip cookies.
            They need 2 and 1/4 cups of all-purpose flour, 1 teaspoon of baking soda,
            1 teaspoon of salt, 1 cup of unsalted butter (softened), 3/4 cup of granulated sugar,
            3/4 cup of packed brown sugar, 1 teaspoon of vanilla extract, and 2 large eggs.
            For the best part, they'll need 2 cups of semisweet chocolate chips.
            First, preheat the oven to 375°F (190°C). Then, in a small bowl, whisk together the flour,
            baking soda, and salt. In a large bowl, cream together the butter, granulated sugar, and brown sugar
            until light and fluffy. Beat in the vanilla and eggs, one at a time. Gradually beat in the dry
            ingredients until just combined. Finally, stir in the chocolate chips. Drop by rounded tablespoons
            onto ungreased baking sheets and bake for 9 to 11 minutes.
            """;

    private final Client client;

    public RecipeExtractorService(Client client) {
        this.client = client;
    }

    /**
     * Builds the JSON schema definition for the Recipe structure.
     */
    public static Map<String, Object> createRecipeSchema() {
        Map<String, Object> ingredientProps = new HashMap<>();

        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        nameProp.put("description", "Name of the ingredient.");
        ingredientProps.put("name", nameProp);

        Map<String, Object> quantityProp = new HashMap<>();
        quantityProp.put("type", "string");
        quantityProp.put("description", "Quantity of the ingredient, including units.");
        ingredientProps.put("quantity", quantityProp);

        Map<String, Object> ingredientItemSchema = new HashMap<>();
        ingredientItemSchema.put("type", "object");
        ingredientItemSchema.put("properties", ingredientProps);
        ingredientItemSchema.put("required", Arrays.asList("name", "quantity"));

        Map<String, Object> properties = new HashMap<>();

        Map<String, Object> recipeNameProp = new HashMap<>();
        recipeNameProp.put("type", "string");
        recipeNameProp.put("description", "The name of the recipe.");
        properties.put("recipe_name", recipeNameProp);

        Map<String, Object> prepTimeProp = new HashMap<>();
        prepTimeProp.put("type", "integer");
        prepTimeProp.put("description", "Optional time in minutes to prepare the recipe.");
        properties.put("prep_time_minutes", prepTimeProp);

        Map<String, Object> ingredientsProp = new HashMap<>();
        ingredientsProp.put("type", "array");
        ingredientsProp.put("items", ingredientItemSchema);
        properties.put("ingredients", ingredientsProp);

        Map<String, Object> instructionsProp = new HashMap<>();
        instructionsProp.put("type", "array");
        Map<String, Object> stringItem = new HashMap<>();
        stringItem.put("type", "string");
        instructionsProp.put("items", stringItem);
        properties.put("instructions", instructionsProp);

        Map<String, Object> recipeJsonSchema = new HashMap<>();
        recipeJsonSchema.put("type", "object");
        recipeJsonSchema.put("properties", properties);
        recipeJsonSchema.put("required", Arrays.asList("recipe_name", "ingredients", "instructions"));

        return recipeJsonSchema;
    }

    /**
     * Extracts a recipe from raw text using Gemini Interactions API and returns a typed Recipe record
     * along with token usage and latency metrics.
     */
    public com.example.gemini.model.ExecutionResult<Recipe> extractRecipeWithMetrics(String textToExtract, String modelName) throws Exception {
        Map<String, Object> schema = createRecipeSchema();

        CreateModelInteractionResponseFormat format =
                CreateModelInteractionResponseFormat.of(
                        ResponseFormat.of(
                                TextResponseFormat.builder()
                                        .mimeType(TextResponseFormatMimeType.APPLICATION_JSON)
                                        .schema(schema)
                                        .build()));

        CreateModelInteraction params =
                CreateModelInteraction.builder()
                        .model(Model.of(modelName != null ? modelName : "gemini-3.8-flash"))
                        .input(InteractionsInput.of(textToExtract))
                        .responseFormat(format)
                        .build();

        long startTime = System.currentTimeMillis();
        Interaction interaction =
                client.interactions.create(CreateInteractionRequestBody.of(params)).interaction().get();
        long durationMs = System.currentTimeMillis() - startTime;

        int inputTokens = interaction.usage().flatMap(com.google.genai.gaos.models.interactions.Usage::totalInputTokens).orElse(0);
        int outputTokens = interaction.usage().flatMap(com.google.genai.gaos.models.interactions.Usage::totalOutputTokens).orElse(0);

        String rawJson = interaction.outputText().orElse("{}");
        Recipe recipe = JsonMapperUtil.fromJson(rawJson, Recipe.class);

        com.example.gemini.model.ExecutionMetrics metrics =
                com.example.gemini.model.ExecutionMetrics.of(inputTokens, outputTokens, durationMs);

        return new com.example.gemini.model.ExecutionResult<>(recipe, metrics);
    }

    /**
     * Extracts a recipe from raw text using Gemini Interactions API and returns a typed Recipe record.
     */
    public Recipe extractRecipe(String textToExtract, String modelName) throws Exception {
        return extractRecipeWithMetrics(textToExtract, modelName).data();
    }

    /**
     * Extracts a recipe with the default model "gemini-3.8-flash".
     */
    public Recipe extractRecipe(String textToExtract) throws Exception {
        return extractRecipe(textToExtract, "gemini-3.8-flash");
    }

    public com.example.gemini.model.ExecutionResult<Recipe> extractRecipeWithMetrics(String textToExtract) throws Exception {
        return extractRecipeWithMetrics(textToExtract, "gemini-3.8-flash");
    }

    /**
     * Returns a realistic mock Recipe for testing without making network calls.
     */
    public static Recipe getMockRecipe() {
        return getMockRecipeWithMetrics().data();
    }

    public static com.example.gemini.model.ExecutionResult<Recipe> getMockRecipeWithMetrics() {
        String mockJson = """
                {
                  "recipe_name": "Delicious Chocolate Chip Cookies",
                  "prep_time_minutes": 15,
                  "ingredients": [
                    { "name": "all-purpose flour", "quantity": "2 and 1/4 cups" },
                    { "name": "baking soda", "quantity": "1 teaspoon" },
                    { "name": "salt", "quantity": "1 teaspoon" },
                    { "name": "unsalted butter (softened)", "quantity": "1 cup" },
                    { "name": "granulated sugar", "quantity": "3/4 cup" },
                    { "name": "packed brown sugar", "quantity": "3/4 cup" },
                    { "name": "vanilla extract", "quantity": "1 teaspoon" },
                    { "name": "large eggs", "quantity": "2" },
                    { "name": "semisweet chocolate chips", "quantity": "2 cups" }
                  ],
                  "instructions": [
                    "Preheat the oven to 375°F (190°C).",
                    "In a small bowl, whisk together the flour, baking soda, and salt.",
                    "In a large bowl, cream together the butter, granulated sugar, and brown sugar until light and fluffy.",
                    "Beat in the vanilla and eggs, one at a time.",
                    "Gradually beat in the dry ingredients until just combined.",
                    "Stir in the chocolate chips.",
                    "Drop by rounded tablespoons onto ungreased baking sheets and bake for 9 to 11 minutes."
                  ]
                }
                """;
        try {
            Recipe recipe = JsonMapperUtil.fromJson(mockJson, Recipe.class);
            com.example.gemini.model.ExecutionMetrics metrics =
                    com.example.gemini.model.ExecutionMetrics.of(245, 182, 620);
            return new com.example.gemini.model.ExecutionResult<>(recipe, metrics);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse mock recipe", e);
        }
    }
}


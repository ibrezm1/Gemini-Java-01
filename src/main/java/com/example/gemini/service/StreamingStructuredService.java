package com.example.gemini.service;

import com.example.gemini.model.Recipe;
import com.example.gemini.util.JsonMapperUtil;
import com.google.genai.Client;
import com.google.genai.gaos.models.interactions.CreateModelInteraction;
import com.google.genai.gaos.models.interactions.CreateModelInteractionResponseFormat;
import com.google.genai.gaos.models.interactions.InteractionSSEEvent;
import com.google.genai.gaos.models.interactions.InteractionSSEStreamEvent;
import com.google.genai.gaos.models.interactions.InteractionsInput;
import com.google.genai.gaos.models.interactions.Model;
import com.google.genai.gaos.models.interactions.ResponseFormat;
import com.google.genai.gaos.models.interactions.StepDelta;
import com.google.genai.gaos.models.interactions.StepDeltaData;
import com.google.genai.gaos.models.interactions.TextDelta;
import com.google.genai.gaos.models.interactions.TextResponseFormat;
import com.google.genai.gaos.models.interactions.TextResponseFormatMimeType;
import com.google.genai.gaos.models.operations.CreateInteractionRequestBody;
import com.google.genai.gaos.models.operations.CreateInteractionResponse;
import com.google.genai.gaos.utils.EventStream;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Demonstrates real-time streaming of structured JSON outputs as documented in the Gemini Interactions API.
 */
public class StreamingStructuredService {

    private final Client client;

    public StreamingStructuredService(Client client) {
        this.client = client;
    }

    /**
     * Streams structured recipe extraction, invoking chunkConsumer with each emitted text fragment,
     * and finally returns the fully assembled and parsed Recipe record along with execution metrics.
     */
    public com.example.gemini.model.ExecutionResult<Recipe> streamExtractRecipeWithMetrics(String promptText, Consumer<String> chunkConsumer, String modelName) throws Exception {
        Map<String, Object> schema = RecipeExtractorService.createRecipeSchema();

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
                        .input(InteractionsInput.of(promptText))
                        .responseFormat(format)
                        .stream(true)
                        .build();

        long startTime = System.currentTimeMillis();
        CreateInteractionResponse response =
                client.interactions.create(CreateInteractionRequestBody.of(params));

        StringBuilder completeJson = new StringBuilder();

        try (EventStream<InteractionSSEStreamEvent> events = response.events()) {
            for (InteractionSSEStreamEvent streamEvent : events) {
                InteractionSSEEvent event = streamEvent.data().orElse(null);
                if (event instanceof StepDelta) {
                    StepDeltaData data = ((StepDelta) event).delta().orElse(null);
                    if (data instanceof TextDelta) {
                        ((TextDelta) data).text().ifPresent(chunk -> {
                            completeJson.append(chunk);
                            if (chunkConsumer != null) {
                                chunkConsumer.accept(chunk);
                            }
                        });
                    }
                }
            }
        }
        long durationMs = System.currentTimeMillis() - startTime;

        Recipe recipe = JsonMapperUtil.fromJson(completeJson.toString(), Recipe.class);

        // Approximate token count based on standard word/character ratio (4 chars ~= 1 token)
        int inputTokens = Math.max(1, promptText.length() / 4);
        int outputTokens = Math.max(1, completeJson.length() / 4);
        com.example.gemini.model.ExecutionMetrics metrics =
                com.example.gemini.model.ExecutionMetrics.of(inputTokens, outputTokens, durationMs);

        return new com.example.gemini.model.ExecutionResult<>(recipe, metrics);
    }

    public Recipe streamExtractRecipe(String promptText, Consumer<String> chunkConsumer, String modelName) throws Exception {
        return streamExtractRecipeWithMetrics(promptText, chunkConsumer, modelName).data();
    }
}


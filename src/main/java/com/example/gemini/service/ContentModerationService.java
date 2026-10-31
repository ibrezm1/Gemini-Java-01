package com.example.gemini.service;

import com.example.gemini.model.ModerationResult;
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
 * Demonstrates Structured Output classification and conditional schema (anyOf)
 * using the Gemini Interactions API.
 */
public class ContentModerationService {

    public static final String DEFAULT_SPAM_SAMPLE =
            "Congratulations! You've won a free cruise to the Bahamas. Click here to claim your prize: www.definitely-not-a-scam.com";

    public static final String DEFAULT_CLEAN_SAMPLE =
            "Hi team, just a reminder that our sprint planning meeting has been moved to Thursday at 2:00 PM.";

    private final Client client;

    public ContentModerationService(Client client) {
        this.client = client;
    }

    /**
     * Builds the JSON schema with anyOf for conditional classification.
     */
    public static Map<String, Object> createModerationSchema() {
        Map<String, Object> spamProps = new HashMap<>();
        Map<String, Object> reasonProp = new HashMap<>();
        reasonProp.put("type", "string");
        reasonProp.put("description", "The reason why the content is considered spam.");
        spamProps.put("reason", reasonProp);

        Map<String, Object> spamTypeProp = new HashMap<>();
        spamTypeProp.put("type", "string");
        spamTypeProp.put("enum", Arrays.asList("phishing", "scam", "unsolicited promotion", "other"));
        spamTypeProp.put("description", "The type of spam.");
        spamProps.put("spam_type", spamTypeProp);

        Map<String, Object> spamDetailsSchema = new HashMap<>();
        spamDetailsSchema.put("type", "object");
        spamDetailsSchema.put("title", "SpamDetails");
        spamDetailsSchema.put("properties", spamProps);
        spamDetailsSchema.put("required", Arrays.asList("reason", "spam_type"));

        Map<String, Object> notSpamProps = new HashMap<>();
        Map<String, Object> summaryProp = new HashMap<>();
        summaryProp.put("type", "string");
        summaryProp.put("description", "A brief summary of the content.");
        notSpamProps.put("summary", summaryProp);

        Map<String, Object> isSafeProp = new HashMap<>();
        isSafeProp.put("type", "boolean");
        isSafeProp.put("description", "Whether the content is safe for all audiences.");
        notSpamProps.put("is_safe", isSafeProp);

        Map<String, Object> notSpamDetailsSchema = new HashMap<>();
        notSpamDetailsSchema.put("type", "object");
        notSpamDetailsSchema.put("title", "NotSpamDetails");
        notSpamDetailsSchema.put("properties", notSpamProps);
        notSpamDetailsSchema.put("required", Arrays.asList("summary", "is_safe"));

        Map<String, Object> decisionProp = new HashMap<>();
        decisionProp.put("anyOf", Arrays.asList(spamDetailsSchema, notSpamDetailsSchema));

        Map<String, Object> properties = new HashMap<>();
        properties.put("decision", decisionProp);

        Map<String, Object> moderationResultJsonSchema = new HashMap<>();
        moderationResultJsonSchema.put("type", "object");
        moderationResultJsonSchema.put("properties", properties);
        moderationResultJsonSchema.put("required", Arrays.asList("decision"));

        return moderationResultJsonSchema;
    }

    /**
     * Moderates the provided content and returns a typed ModerationResult record along with execution metrics.
     */
    public com.example.gemini.model.ExecutionResult<ModerationResult> moderateWithMetrics(String content, String modelName) throws Exception {
        Map<String, Object> schema = createModerationSchema();

        String prompt = "Please moderate the following content and provide a decision.\nContent: '"
                + content.replace("'", "''") + "'";

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
                        .input(InteractionsInput.of(prompt))
                        .responseFormat(format)
                        .build();

        long startTime = System.currentTimeMillis();
        Interaction interaction =
                client.interactions.create(CreateInteractionRequestBody.of(params)).interaction().get();
        long durationMs = System.currentTimeMillis() - startTime;

        int inputTokens = interaction.usage().flatMap(com.google.genai.gaos.models.interactions.Usage::totalInputTokens).orElse(0);
        int outputTokens = interaction.usage().flatMap(com.google.genai.gaos.models.interactions.Usage::totalOutputTokens).orElse(0);

        String rawJson = interaction.outputText().orElse("{}");
        ModerationResult result = JsonMapperUtil.fromJson(rawJson, ModerationResult.class);

        com.example.gemini.model.ExecutionMetrics metrics =
                com.example.gemini.model.ExecutionMetrics.of(inputTokens, outputTokens, durationMs);

        return new com.example.gemini.model.ExecutionResult<>(result, metrics);
    }

    /**
     * Moderates the provided content and returns a typed ModerationResult record.
     */
    public ModerationResult moderate(String content, String modelName) throws Exception {
        return moderateWithMetrics(content, modelName).data();
    }

    public ModerationResult moderate(String content) throws Exception {
        return moderate(content, "gemini-3.8-flash");
    }

    public com.example.gemini.model.ExecutionResult<ModerationResult> moderateWithMetrics(String content) throws Exception {
        return moderateWithMetrics(content, "gemini-3.8-flash");
    }

    public static ModerationResult getMockModeration(boolean isSpam) {
        return getMockModerationWithMetrics(isSpam).data();
    }

    public static com.example.gemini.model.ExecutionResult<ModerationResult> getMockModerationWithMetrics(boolean isSpam) {
        String mockJson = isSpam ? """
                {
                  "decision": {
                    "reason": "The content is an unsolicited prize notification attempting to trick the user into clicking a suspicious link.",
                    "spam_type": "scam"
                  }
                }
                """ : """
                {
                  "decision": {
                    "summary": "Internal team reminder regarding meeting scheduling.",
                    "is_safe": true
                  }
                }
                """;
        try {
            ModerationResult result = JsonMapperUtil.fromJson(mockJson, ModerationResult.class);
            com.example.gemini.model.ExecutionMetrics metrics =
                    com.example.gemini.model.ExecutionMetrics.of(120, 48, 410);
            return new com.example.gemini.model.ExecutionResult<>(result, metrics);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse mock moderation", e);
        }
    }
}


package com.example.gemini.service;

import com.example.gemini.model.MovieAnalysis;
import com.example.gemini.model.Sentiment;
import com.example.gemini.util.JsonMapperUtil;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates Structured Output using client.models.generateContent(...)
 * with GenerateContentConfig and com.google.genai.types.Schema.
 */
public class MovieAnalysisService {

    public static final String DEFAULT_MOVIE_REVIEW =
            """
            Inception (2010), directed by Christopher Nolan, is an astonishing cinematic achievement.
            The dream-within-a-dream concept keeps viewers on the edge of their seats throughout
            the entire 148-minute runtime. Leonardo DiCaprio delivers a gripping performance as Cobb,
            supported by a phenomenal ensemble including Joseph Gordon-Levitt and Marion Cotillard.
            Hans Zimmer's soundtrack elevates every scene, creating an unforgettable sensory experience.
            While the plot requires intense concentration, the emotional payoff and breathtaking
            visual effects make it an absolute masterpiece. I'd easily rate it 9.3 out of 10!
            """;

    private final Client client;

    public MovieAnalysisService(Client client) {
        this.client = client;
    }

    /**
     * Builds a strongly typed SDK Schema object.
     */
    public static Schema createMovieSchema() {
        Map<String, Schema> props = new HashMap<>();

        props.put("title", Schema.builder()
                .type("STRING")
                .description("The title of the film.")
                .build());

        props.put("release_year", Schema.builder()
                .type("INTEGER")
                .description("The release year of the film.")
                .build());

        props.put("sentiment", Schema.builder()
                .type("STRING")
                .enum_(Arrays.asList("POSITIVE", "NEUTRAL", "NEGATIVE"))
                .description("The overall sentiment of the review.")
                .build());

        props.put("rating_out_of_10", Schema.builder()
                .type("NUMBER")
                .description("Extracted or inferred numerical score out of 10.")
                .build());

        props.put("key_themes", Schema.builder()
                .type("ARRAY")
                .items(Schema.builder().type("STRING").build())
                .description("Key thematic elements mentioned in the review.")
                .build());

        props.put("summary", Schema.builder()
                .type("STRING")
                .description("A concise 2-sentence summary of the review.")
                .build());

        return Schema.builder()
                .type("OBJECT")
                .properties(props)
                .required(Arrays.asList("title", "sentiment", "summary"))
                .build();
    }

    /**
     * Analyzes a movie review and returns a structured MovieAnalysis record along with execution metrics.
     */
    public com.example.gemini.model.ExecutionResult<MovieAnalysis> analyzeWithMetrics(String reviewText, String modelName) throws Exception {
        Schema schema = createMovieSchema();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(schema)
                .temperature(0.2f)
                .build();

        String prompt = "Analyze the following movie review and extract structured details:\n\n" + reviewText;

        long startTime = System.currentTimeMillis();
        GenerateContentResponse response = client.models.generateContent(
                modelName != null ? modelName : "gemini-3.8-flash",
                prompt,
                config
        );
        long durationMs = System.currentTimeMillis() - startTime;

        int inputTokens = response.usageMetadata().flatMap(com.google.genai.types.GenerateContentResponseUsageMetadata::promptTokenCount).orElse(0);
        int outputTokens = response.usageMetadata().flatMap(com.google.genai.types.GenerateContentResponseUsageMetadata::candidatesTokenCount).orElse(0);

        String rawJson = response.text();
        MovieAnalysis analysis = JsonMapperUtil.fromJson(rawJson, MovieAnalysis.class);

        com.example.gemini.model.ExecutionMetrics metrics =
                com.example.gemini.model.ExecutionMetrics.of(inputTokens, outputTokens, durationMs);

        return new com.example.gemini.model.ExecutionResult<>(analysis, metrics);
    }

    /**
     * Analyzes a movie review and returns a structured MovieAnalysis record.
     */
    public MovieAnalysis analyze(String reviewText, String modelName) throws Exception {
        return analyzeWithMetrics(reviewText, modelName).data();
    }

    public MovieAnalysis analyze(String reviewText) throws Exception {
        return analyze(reviewText, "gemini-3.8-flash");
    }

    public com.example.gemini.model.ExecutionResult<MovieAnalysis> analyzeWithMetrics(String reviewText) throws Exception {
        return analyzeWithMetrics(reviewText, "gemini-3.8-flash");
    }

    public static MovieAnalysis getMockAnalysis() {
        return getMockAnalysisWithMetrics().data();
    }

    public static com.example.gemini.model.ExecutionResult<MovieAnalysis> getMockAnalysisWithMetrics() {
        String mockJson = """
                {
                  "title": "Inception",
                  "release_year": 2010,
                  "sentiment": "POSITIVE",
                  "rating_out_of_10": 9.3,
                  "key_themes": [
                    "Dreams vs. Reality",
                    "Grief and Memory",
                    "Heist Strategy",
                    "Subconscious Manipulation"
                  ],
                  "summary": "Inception is a visually breathtaking and intellectually captivating sci-fi thriller about entering human dreams. Christopher Nolan crafts a masterclass in tension, anchored by superb performances and Hans Zimmer's iconic score."
                }
                """;
        try {
            MovieAnalysis analysis = JsonMapperUtil.fromJson(mockJson, MovieAnalysis.class);
            com.example.gemini.model.ExecutionMetrics metrics =
                    com.example.gemini.model.ExecutionMetrics.of(185, 96, 510);
            return new com.example.gemini.model.ExecutionResult<>(analysis, metrics);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse mock analysis", e);
        }
    }
}


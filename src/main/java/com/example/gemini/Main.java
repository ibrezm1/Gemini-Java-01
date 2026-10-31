package com.example.gemini;

import com.example.gemini.config.GeminiClientFactory;
import com.example.gemini.model.ModerationResult;
import com.example.gemini.model.MovieAnalysis;
import com.example.gemini.model.Recipe;
import com.example.gemini.service.ContentModerationService;
import com.example.gemini.service.MovieAnalysisService;
import com.example.gemini.service.RecipeExtractorService;
import com.example.gemini.service.StreamingStructuredService;
import com.example.gemini.util.JsonMapperUtil;
import com.google.genai.Client;

import java.util.Optional;
import java.util.Scanner;

/**
 * Main application demonstrating structured response extraction with the Gemini API in Java.
 */
public class Main {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BOLD = "\u001B[1m";

    public static void main(String[] args) {
        printBanner();

        Optional<Client> clientOpt = GeminiClientFactory.tryCreateClient();
        boolean isLive = clientOpt.isPresent();

        if (isLive) {
            System.out.println(ANSI_GREEN + "✔ Connected to Gemini API (API key detected)." + ANSI_RESET);
        } else {
            System.out.println(ANSI_YELLOW + "⚠ No GEMINI_API_KEY detected." + ANSI_RESET);
            System.out.println("  The app will demonstrate schema validation & Jackson record parsing using simulation mode.");
            System.out.println("  To make live calls to Gemini: export GEMINI_API_KEY=\"your_key_here\"\n");
        }

        if (args.length > 0) {
            handleCliArgs(args, clientOpt);
            return;
        }

        runInteractiveMenu(clientOpt);
    }

    private static void handleCliArgs(String[] args, Optional<Client> clientOpt) {
        String command = args[0].toLowerCase();
        try {
            switch (command) {
                case "recipe" -> {
                    String text = args.length > 1 ? args[1] : RecipeExtractorService.DEFAULT_RECIPE_TEXT;
                    demoRecipe(clientOpt, text);
                }
                case "moderate" -> {
                    String text = args.length > 1 ? args[1] : ContentModerationService.DEFAULT_SPAM_SAMPLE;
                    demoModeration(clientOpt, text);
                }
                case "movie" -> {
                    String text = args.length > 1 ? args[1] : MovieAnalysisService.DEFAULT_MOVIE_REVIEW;
                    demoMovie(clientOpt, text);
                }
                case "stream" -> demoStreaming(clientOpt);
                case "all", "demo" -> runAllDemos(clientOpt);
                default -> {
                    System.out.println("Unknown command: " + command);
                    System.out.println("Usage: java -jar app.jar [recipe|moderate|movie|stream|all] [input-text]");
                }
            }
        } catch (Exception e) {
            System.err.println("Error executing command " + command + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runInteractiveMenu(Optional<Client> clientOpt) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n" + ANSI_BOLD + "=== Gemini API Structured Response Demos ===" + ANSI_RESET);
            System.out.println("1. Recipe Extraction (Interactions API -> Recipe Record)");
            System.out.println("2. Content Moderation (Conditional Schema anyOf -> ModerationResult Record)");
            System.out.println("3. Movie Review Analysis (GenerateContent API -> MovieAnalysis Record)");
            System.out.println("4. Streaming Structured Output (Live tokens -> parsed Record)");
            System.out.println("5. Run All Demos");
            System.out.println("6. Exit");
            System.out.print(ANSI_CYAN + "Choose an option (1-6): " + ANSI_RESET);

            String choice = scanner.nextLine().trim();
            if ("6".equals(choice) || "exit".equalsIgnoreCase(choice) || "q".equalsIgnoreCase(choice)) {
                System.out.println("Goodbye!");
                break;
            }

            try {
                switch (choice) {
                    case "1" -> demoRecipe(clientOpt, RecipeExtractorService.DEFAULT_RECIPE_TEXT);
                    case "2" -> {
                        System.out.println("\nTesting with Spam Message:");
                        demoModeration(clientOpt, ContentModerationService.DEFAULT_SPAM_SAMPLE);
                        System.out.println("\nTesting with Clean Message:");
                        demoModeration(clientOpt, ContentModerationService.DEFAULT_CLEAN_SAMPLE);
                    }
                    case "3" -> demoMovie(clientOpt, MovieAnalysisService.DEFAULT_MOVIE_REVIEW);
                    case "4" -> demoStreaming(clientOpt);
                    case "5" -> runAllDemos(clientOpt);
                    default -> System.out.println("Invalid option, please choose 1-6.");
                }
            } catch (Exception e) {
                System.err.println("Execution failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void demoRecipe(Optional<Client> clientOpt, String input) throws Exception {
        System.out.println("\n" + ANSI_BOLD + "▶ [Demo 1] Recipe Extraction via Interactions API" + ANSI_RESET);
        System.out.println("Extracting structured JSON into `Recipe` and `List<Ingredient>` records...\n");

        com.example.gemini.model.ExecutionResult<Recipe> result;
        if (clientOpt.isPresent()) {
            System.out.println("Calling Gemini API (`gemini-3.8-flash`)...");
            RecipeExtractorService service = new RecipeExtractorService(clientOpt.get());
            result = service.extractRecipeWithMetrics(input);
        } else {
            System.out.println("[Simulation Mode] Using sample model response:");
            result = RecipeExtractorService.getMockRecipeWithMetrics();
        }

        Recipe recipe = result.data();
        System.out.println(ANSI_GREEN + "✔ Structured JSON Output parsed into Java Record:" + ANSI_RESET);
        System.out.println(JsonMapperUtil.toPrettyJson(recipe));

        System.out.println("\n" + ANSI_BOLD + "Type-Safe Java Object Access:" + ANSI_RESET);
        System.out.println("  • Recipe Name: " + recipe.recipeName());
        System.out.println("  • Prep Time:   " + recipe.prepTimeMinutes() + " minutes");
        System.out.println("  • Ingredients (" + recipe.ingredients().size() + "):");
        recipe.ingredients().forEach(ing -> System.out.println("     - " + ing.name() + " (" + ing.quantity() + ")"));
        System.out.println("  • Steps (" + recipe.instructions().size() + "):");
        recipe.instructions().forEach(step -> System.out.println("     1. " + step));

        // Print input/output tokens, time taken, and cost
        result.metrics().printSummary();
    }

    private static void demoModeration(Optional<Client> clientOpt, String input) throws Exception {
        System.out.println("\n" + ANSI_BOLD + "▶ [Demo 2] Content Moderation (anyOf Schema)" + ANSI_RESET);
        System.out.println("Input: \"" + input + "\"");

        com.example.gemini.model.ExecutionResult<ModerationResult> execResult;
        if (clientOpt.isPresent()) {
            System.out.println("Calling Gemini API (`gemini-3.8-flash`)...");
            ContentModerationService service = new ContentModerationService(clientOpt.get());
            execResult = service.moderateWithMetrics(input);
        } else {
            boolean isSpam = input.toLowerCase().contains("congratulations") || input.toLowerCase().contains("bahamas");
            execResult = ContentModerationService.getMockModerationWithMetrics(isSpam);
        }

        ModerationResult result = execResult.data();
        System.out.println(ANSI_GREEN + "✔ Result JSON parsed into Java Record:" + ANSI_RESET);
        System.out.println(JsonMapperUtil.toPrettyJson(result));

        var decision = result.decision();
        if (decision.isSpam()) {
            System.out.println(ANSI_YELLOW + "  [SPAM DETECTED] Type: " + decision.spamType() + ANSI_RESET);
            System.out.println("  Reason: " + decision.reason());
        } else {
            System.out.println(ANSI_GREEN + "  [CONTENT SAFE] isSafe: " + decision.isSafe() + ANSI_RESET);
            System.out.println("  Summary: " + decision.summary());
        }

        // Print input/output tokens, time taken, and cost
        execResult.metrics().printSummary();
    }

    private static void demoMovie(Optional<Client> clientOpt, String input) throws Exception {
        System.out.println("\n" + ANSI_BOLD + "▶ [Demo 3] Movie Analysis via client.models.generateContent" + ANSI_RESET);
        System.out.println("Configured with GenerateContentConfig & com.google.genai.types.Schema...\n");

        com.example.gemini.model.ExecutionResult<MovieAnalysis> result;
        if (clientOpt.isPresent()) {
            System.out.println("Calling Gemini API (`gemini-3.8-flash`)...");
            MovieAnalysisService service = new MovieAnalysisService(clientOpt.get());
            result = service.analyzeWithMetrics(input);
        } else {
            result = MovieAnalysisService.getMockAnalysisWithMetrics();
        }

        MovieAnalysis analysis = result.data();
        System.out.println(ANSI_GREEN + "✔ Structured JSON Output parsed into Java Record:" + ANSI_RESET);
        System.out.println(JsonMapperUtil.toPrettyJson(analysis));

        System.out.println("\n" + ANSI_BOLD + "Type-Safe Java Object Access:" + ANSI_RESET);
        System.out.println("  • Film Title:   " + analysis.title());
        System.out.println("  • Year:         " + analysis.releaseYear());
        System.out.println("  • Sentiment:    " + analysis.sentiment());
        System.out.println("  • Rating:       " + analysis.ratingOutOf10() + "/10");
        System.out.println("  • Themes:       " + String.join(", ", analysis.keyThemes()));
        System.out.println("  • Summary:      " + analysis.summary());

        // Print input/output tokens, time taken, and cost
        result.metrics().printSummary();
    }

    private static void demoStreaming(Optional<Client> clientOpt) throws Exception {
        System.out.println("\n" + ANSI_BOLD + "▶ [Demo 4] Streaming Structured Output (Live EventStream)" + ANSI_RESET);

        com.example.gemini.model.ExecutionResult<Recipe> result;
        if (clientOpt.isPresent()) {
            System.out.println("Streaming tokens in real-time from Gemini...");
            StreamingStructuredService service = new StreamingStructuredService(clientOpt.get());
            System.out.print(ANSI_CYAN);
            result = service.streamExtractRecipeWithMetrics(
                    RecipeExtractorService.DEFAULT_RECIPE_TEXT,
                    chunk -> System.out.print(chunk),
                    "gemini-3.8-flash"
            );
            System.out.print(ANSI_RESET + "\n\n");
            System.out.println(ANSI_GREEN + "✔ Stream completed and successfully validated as Recipe record: "
                    + result.data().recipeName() + ANSI_RESET);
        } else {
            System.out.println("[Simulation Mode] Simulating token stream chunks:");
            String json = JsonMapperUtil.toPrettyJson(RecipeExtractorService.getMockRecipe());
            System.out.print(ANSI_CYAN);
            long start = System.currentTimeMillis();
            for (String line : json.split("\n")) {
                System.out.println(line);
                Thread.sleep(60);
            }
            long dur = System.currentTimeMillis() - start;
            System.out.print(ANSI_RESET);
            System.out.println(ANSI_GREEN + "✔ Simulation stream parsed into Recipe record successfully!" + ANSI_RESET);
            result = new com.example.gemini.model.ExecutionResult<>(
                    RecipeExtractorService.getMockRecipe(),
                    com.example.gemini.model.ExecutionMetrics.of(245, 182, dur)
            );
        }

        // Print input/output tokens, time taken, and cost
        result.metrics().printSummary();
    }

    private static void runAllDemos(Optional<Client> clientOpt) throws Exception {
        demoRecipe(clientOpt, RecipeExtractorService.DEFAULT_RECIPE_TEXT);
        System.out.println("\n" + "=".repeat(60));
        demoModeration(clientOpt, ContentModerationService.DEFAULT_SPAM_SAMPLE);
        System.out.println("\n" + "=".repeat(60));
        demoMovie(clientOpt, MovieAnalysisService.DEFAULT_MOVIE_REVIEW);
        System.out.println("\n" + "=".repeat(60));
        demoStreaming(clientOpt);
    }

    private static void printBanner() {
        System.out.println(ANSI_CYAN + """
                 ___ ___ _____ ___ _  _ ___   ___ _                     _                  _   \s
                / __| __|_   _|_ _| \\| |_ _| |   \\ |__ ___  _ _ _  _ __| |_ _  _ _ _ ___ _| |  \s
               | (_ | _|  | |  | || .` || |  | |) / _` / _|| '_| || / _|  _| || | '_/ -_) _` |  \s
                \\___|___| |_| |___|_|\\_|___| |___/\\__,_\\__||_|  \\_,_\\__|\\__|\\_,_|_| \\___\\__,_|  \s
                                 Official Google Gen AI Java SDK Demo
                """ + ANSI_RESET);
    }
}

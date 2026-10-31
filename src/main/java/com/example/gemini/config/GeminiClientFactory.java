package com.example.gemini.config;

import com.google.genai.Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class GeminiClientFactory {

    private GeminiClientFactory() {}

    /**
     * Resolves the Gemini API key from environment variables, system properties, or a .env file.
     */
    public static Optional<String> resolveApiKey() {
        // 1. GEMINI_API_KEY env var
        String key = System.getenv("GEMINI_API_KEY");
        if (isValidKey(key)) return Optional.of(key.trim());

        // 2. GOOGLE_API_KEY env var
        key = System.getenv("GOOGLE_API_KEY");
        if (isValidKey(key)) return Optional.of(key.trim());

        // 3. System property -Dgemini.api.key=...
        key = System.getProperty("gemini.api.key");
        if (isValidKey(key)) return Optional.of(key.trim());

        // 4. Try reading from a local .env file
        Path envFile = Path.of(".env");
        if (Files.isRegularFile(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("GEMINI_API_KEY=")) {
                        String val = trimmed.substring("GEMINI_API_KEY=".length()).trim();
                        val = unquote(val);
                        if (isValidKey(val)) return Optional.of(val);
                    } else if (trimmed.startsWith("GOOGLE_API_KEY=")) {
                        String val = trimmed.substring("GOOGLE_API_KEY=".length()).trim();
                        val = unquote(val);
                        if (isValidKey(val)) return Optional.of(val);
                    }
                }
            } catch (IOException ignored) {
                // Ignore failure reading .env
            }
        }

        return Optional.empty();
    }

    public static boolean hasApiKey() {
        return resolveApiKey().isPresent();
    }

    /**
     * Creates a new configured Client instance.
     * Throws IllegalStateException if no API key is available.
     */
    public static Client createClient() {
        Optional<String> apiKey = resolveApiKey();
        if (apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "No Gemini API key found. Please set GEMINI_API_KEY in your environment or provide it in a .env file."
            );
        }
        return Client.builder().apiKey(apiKey.get()).build();
    }

    /**
     * Creates a Client if an API key is available, or empty Optional otherwise.
     */
    public static Optional<Client> tryCreateClient() {
        return resolveApiKey().map(key -> Client.builder().apiKey(key).build());
    }

    private static boolean isValidKey(String key) {
        return key != null && !key.isBlank() && !key.equals("YOUR_API_KEY");
    }

    private static String unquote(String val) {
        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }
}


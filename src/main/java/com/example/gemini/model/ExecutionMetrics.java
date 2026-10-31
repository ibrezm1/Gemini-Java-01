package com.example.gemini.model;

import java.text.DecimalFormat;

/**
 * Tracks token usage, latency, and estimated cost for a Gemini API call.
 */
public record ExecutionMetrics(
        int inputTokens,
        int outputTokens,
        int totalTokens,
        long durationMs,
        double estimatedCostUsd
) {
    // Official pricing for Gemini Flash models (USD per million tokens)
    public static final double INPUT_COST_PER_MILLION = 0.10;   // $0.10 / 1M prompt tokens
    public static final double OUTPUT_COST_PER_MILLION = 0.40;  // $0.40 / 1M output tokens

    private static final DecimalFormat COST_FORMAT = new DecimalFormat("$0.000000");
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("0.00");

    public static ExecutionMetrics of(int inputTokens, int outputTokens, long durationMs) {
        int total = inputTokens + outputTokens;
        double cost = (inputTokens * (INPUT_COST_PER_MILLION / 1_000_000.0))
                + (outputTokens * (OUTPUT_COST_PER_MILLION / 1_000_000.0));
        return new ExecutionMetrics(inputTokens, outputTokens, total, durationMs, cost);
    }

    public String formattedCost() {
        return COST_FORMAT.format(estimatedCostUsd) + " USD";
    }

    public String formattedDuration() {
        double seconds = durationMs / 1000.0;
        return durationMs + " ms (" + TIME_FORMAT.format(seconds) + " s)";
    }

    /**
     * Prints a formatted metrics summary block to standard out.
     */
    public void printSummary() {
        String reset = "\u001B[0m";
        String cyan = "\u001B[36m";
        String bold = "\u001B[1m";
        String green = "\u001B[32m";

        System.out.println("\n" + bold + "⏱️  Gemini Execution Metrics:" + reset);
        System.out.println("  • " + cyan + "Input Tokens:" + reset + "     " + inputTokens);
        System.out.println("  • " + cyan + "Output Tokens:" + reset + "    " + outputTokens);
        System.out.println("  • " + cyan + "Total Tokens:" + reset + "     " + totalTokens);
        System.out.println("  • " + cyan + "Time Taken:" + reset + "       " + formattedDuration());
        System.out.println("  • " + green + "Estimated Cost:" + reset + "   " + bold + formattedCost() + reset);
    }
}


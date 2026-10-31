package com.example.gemini.model;

/**
 * Encapsulates the deserialized response data alongside execution metrics.
 */
public record ExecutionResult<T>(
        T data,
        ExecutionMetrics metrics
) {}


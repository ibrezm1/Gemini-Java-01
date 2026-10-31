package com.example.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModerationResult(
        @JsonProperty("decision")
        ModerationDecision decision
) {}


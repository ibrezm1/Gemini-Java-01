package com.example.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ModerationDecision(
        @JsonProperty("reason")
        String reason,

        @JsonProperty("spam_type")
        String spamType,

        @JsonProperty("summary")
        String summary,

        @JsonProperty("is_safe")
        Boolean isSafe
) {
    public boolean isSpam() {
        return spamType != null && !spamType.isBlank();
    }
}


package com.example.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Sentiment {
    @JsonProperty("POSITIVE")
    POSITIVE,

    @JsonProperty("NEUTRAL")
    NEUTRAL,

    @JsonProperty("NEGATIVE")
    NEGATIVE
}


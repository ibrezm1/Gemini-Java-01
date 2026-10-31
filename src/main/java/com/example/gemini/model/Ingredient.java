package com.example.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Ingredient(
        @JsonProperty("name") String name,
        @JsonProperty("quantity") String quantity
) {}


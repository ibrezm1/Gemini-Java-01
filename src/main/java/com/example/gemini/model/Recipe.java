package com.example.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Recipe(
        @JsonProperty(value = "recipe_name", required = true)
        String recipeName,

        @JsonProperty("prep_time_minutes")
        Integer prepTimeMinutes,

        @JsonProperty(value = "ingredients", required = true)
        List<Ingredient> ingredients,

        @JsonProperty(value = "instructions", required = true)
        List<String> instructions
) {}


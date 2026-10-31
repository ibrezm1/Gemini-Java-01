package com.example.gemini.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MovieAnalysis(
        @JsonProperty(value = "title", required = true)
        String title,

        @JsonProperty("release_year")
        Integer releaseYear,

        @JsonProperty(value = "sentiment", required = true)
        Sentiment sentiment,

        @JsonProperty("rating_out_of_10")
        Double ratingOutOf10,

        @JsonProperty("key_themes")
        List<String> keyThemes,

        @JsonProperty(value = "summary", required = true)
        String summary
) {}


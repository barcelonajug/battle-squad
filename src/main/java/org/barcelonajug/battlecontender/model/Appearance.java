package org.barcelonajug.battlecontender.model;

public record Appearance(
        String gender,
        String race,
        Integer heightCm,
        Integer weightKg,
        String eyeColor,
        String hairColor
) {}

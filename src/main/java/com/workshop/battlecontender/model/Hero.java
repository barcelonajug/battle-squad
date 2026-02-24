package com.workshop.battlecontender.model;

import java.util.List;

public record Hero(
        int id,
        String name,
        String slug,
        PowerStats powerstats,
        String role,
        int cost,
        String alignment,
        String publisher,
        Appearance appearance,
        Biography biography,
        List<String> tags,
        Images images) {
}

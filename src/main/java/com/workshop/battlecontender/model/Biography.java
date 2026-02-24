package com.workshop.battlecontender.model;

import java.util.List;

public record Biography(
        String fullName,
        String placeOfBirth,
        String firstAppearance,
        List<String> aliases) {
}

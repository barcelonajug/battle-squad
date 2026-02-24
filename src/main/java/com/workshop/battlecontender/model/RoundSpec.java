package com.workshop.battlecontender.model;

import java.util.List;
import java.util.Map;

public record RoundSpec(
        String description,
        int teamSize,
        int budgetCap,
        Map<String, Integer> requiredRoles,
        Map<String, Integer> maxSameRole,
        List<String> bannedTags,
        Map<String, Double> tagModifiers,
        String mapType) {
}

package org.barcelonajug.battlecontender.model;

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
        String mapType,
        List<String> allowedRoles,
        List<String> allowedGenders,
        List<String> allowedRaces,
        List<String> allowedPublishers,
        List<String> allowedAlignments) {

    public RoundSpec(
            String description,
            int teamSize,
            int budgetCap,
            Map<String, Integer> requiredRoles,
            Map<String, Integer> maxSameRole,
            List<String> bannedTags,
            Map<String, Double> tagModifiers,
            String mapType) {
        this(description, teamSize, budgetCap, requiredRoles, maxSameRole, bannedTags, tagModifiers, mapType,
                null, null, null, null, null);
    }
}

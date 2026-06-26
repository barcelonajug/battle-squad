package org.barcelonajug.battlecontender.client;

public record AdvancedHeroSearchCriteria(
        String name,
        String alignment,
        String publisher,
        String role,
        String gender,
        String race,
        Integer minCost,
        Integer maxCost,
        Integer minPower,
        Integer maxPower,
        Integer minStrength,
        Integer maxStrength,
        Integer minSpeed,
        Integer maxSpeed,
        Integer minIntelligence,
        Integer maxIntelligence,
        Integer minDurability,
        Integer maxDurability,
        Integer minCombat,
        Integer maxCombat,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection) {
}

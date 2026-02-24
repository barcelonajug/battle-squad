package com.workshop.battlecontender.ai;

import java.util.List;

public record SquadRecommendation(
        List<Integer> heroIds,
        String strategy,
        String reasoning,
        int totalCost) {
}

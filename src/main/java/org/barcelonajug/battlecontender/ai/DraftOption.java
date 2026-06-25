package org.barcelonajug.battlecontender.ai;

import java.util.List;

public record DraftOption(
        String strategyId,
        String label,
        String summary,
        SquadRecommendation recommendation,
        boolean recommended,
        boolean valid,
        List<String> violations) {
}

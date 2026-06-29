package org.barcelonajug.battlecontender.ai;

public record SubagentDraft(
        String strategyId,
        SquadRecommendation recommendation,
        String error) {
}

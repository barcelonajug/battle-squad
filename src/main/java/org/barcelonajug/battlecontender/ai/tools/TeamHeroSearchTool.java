package org.barcelonajug.battlecontender.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.UUID;

public final class TeamHeroSearchTool {

    private final HeroSearchTool heroSearchTool;
    private final UUID teamId;
    private final String strategyId;

    public TeamHeroSearchTool(HeroSearchTool heroSearchTool, UUID teamId, String strategyId) {
        this.heroSearchTool = heroSearchTool;
        this.teamId = teamId;
        this.strategyId = strategyId;
    }

    @Tool(description = "Find a varied, team-specific set of heroes eligible for this round. Choose the squad from this set.")
    public List<HeroSearchTool.HeroSummary> findHeroesForRound(
            @ToolParam(description = "Session UUID as a string") String sessionId,
            @ToolParam(description = "Round number") int roundNo) {
        return heroSearchTool.findHeroesForTeamRound(teamId, UUID.fromString(sessionId), roundNo, strategyId);
    }
}

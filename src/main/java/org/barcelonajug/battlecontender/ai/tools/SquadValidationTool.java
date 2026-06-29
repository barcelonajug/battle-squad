package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.ai.SquadValidationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SquadValidationTool {

    private final SquadValidationService squadValidationService;

    public SquadValidationTool(SquadValidationService squadValidationService) {
        this.squadValidationService = squadValidationService;
    }

    @Tool(description = """
            Validate a proposed squad against every deterministic rule for a round. Call this before returning a
            recommendation. If it is invalid, use the violations to correct the squad and validate once more.
            """)
    public SquadValidationService.ValidationResult validateSquadForRound(
            @ToolParam(description = "Session UUID as a string") String sessionId,
            @ToolParam(description = "Round number") int roundNo,
            @ToolParam(description = "Exactly the proposed hero IDs") List<Integer> heroIds) {
        return squadValidationService.validateSquad(UUID.fromString(sessionId), roundNo, heroIds);
    }
}

package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.barcelonajug.battlecontender.model.Session;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ArenaManagementTool {

    private final ArenaApiClient arenaApiClient;

    public ArenaManagementTool(ArenaApiClient arenaApiClient) {
        this.arenaApiClient = arenaApiClient;
    }

    @Tool(description = "Get the currently active tournament session ID and details.")
    public Session getActiveSession() {
        // TODO: Call arenaApiClient.getActiveSession()
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }

    @Tool(description = "Get the constraints and rules for a specific round.")
    public RoundSpec getRoundConstraints(
            @ToolParam(description = "Number of the round") int roundNo,
            @ToolParam(description = "ID of the tournament session") String sessionId) {
        // TODO: Parse UUID and call arenaApiClient.getRound(roundNo, parsedSessionId)
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }
}

package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BattleAdvisorService {

    private final ChatClient chatClient;
    private final HeroSearchTool heroSearchTool;
    private final ArenaManagementTool arenaManagementTool;

    public BattleAdvisorService(ChatClient.Builder chatClientBuilder,
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool) {
        this.chatClient = chatClientBuilder.build();
        this.heroSearchTool = heroSearchTool;
        this.arenaManagementTool = arenaManagementTool;
    }

    public SquadRecommendation buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId) {
        // TODO: Workshop Participant Implementation
        // 1. Define a system prompt template explaining the game rules.
        // 2. Use this.chatClient.prompt() with the system prompt, passing roundNo and
        // sessionId as parameters.
        // 3. Register the tools: heroSearchTool and arenaManagementTool.
        // 4. Optionally, add SimpleLoggerAdvisor for observability.
        // 5. Use .call().entity(SquadRecommendation.class) to get structured JSON
        // output.

        throw new UnsupportedOperationException("TODO: Implement the AI advisor using Spring AI ChatClient");
    }
}

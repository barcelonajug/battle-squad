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
        return chatClient.prompt()
                .system(s -> s.text("""
                        You are an expert superhero team advisor.
                        The user is participating in round {roundNo} of session {sessionId}.
                        Your goal is to build an optimal squad of exactly 3 heroes.
                        Use the arena tool to get the round constraints and the available budget.
                        Search for heroes using the hero search tool.
                        Pick the best combination of 3 heroes that fits the budget and rules.
                        Provide a detailed strategy and reasoning for your choice.
                        """)
                        .param("roundNo", roundNo)
                        .param("sessionId", sessionId.toString()))
                .tools(heroSearchTool, arenaManagementTool)
                .call()
                .entity(SquadRecommendation.class);
    }
}

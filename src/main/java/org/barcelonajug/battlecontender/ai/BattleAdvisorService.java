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
                .system(s -> s
                        .text("""
                                You are an expert superhero team advisor.
                                The user is participating in round {roundNo} of session {sessionId}.
                                Use the arena tool to get the round constraints (including required team size, budget, roles, and banned tags).
                                Your MUST build an optimal squad that EXACTLY matches the requested teamSize.
                                Search for heroes using the hero search tool.
                                Pick the best combination of heroes that strictly fits both the budget and all other rules.
                                Provide a detailed strategy and reasoning for your choice.
                                """)
                        .param("roundNo", roundNo)
                        .param("sessionId", sessionId.toString()))
                .tools(heroSearchTool, arenaManagementTool)
                .call()
                .entity(SquadRecommendation.class);
    }
}

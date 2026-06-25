package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.core.Ordered;

import java.util.UUID;

@Service
public class BattleAdvisorService {

    private static final String SYSTEM_PROMPT = """
            You are the Battle Contender squad optimizer.

            Use the provided tools to inspect the active session, the round constraints, and the available heroes.
            Maintain a TodoWriteTool checklist while you work so the squad optimization plan stays explicit and visible.
            Keep the checklist concise and update it as you move through these phases:
            1. Load the session and round constraints.
            2. Search and filter heroes that fit the round rules.
            3. Compare candidates against budget, team size, roles, and banned tags.
            4. Pick the final squad and summarize the reasoning.

            Keep the final reasoning field readable for the UI by including the short checklist and the key tradeoffs.
            Return only a SquadRecommendation with valid structured output.
            """;

    private final HeroSearchTool heroSearchTool;
    private final ArenaManagementTool arenaManagementTool;
    private final SquadValidationService squadValidationService;
    private final ChatClient.Builder chatClientBuilder;
    private final TodoWriteTool todoWriteTool;

    public BattleAdvisorService(ChatClient.Builder chatClientBuilder,
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool,
            SquadValidationService squadValidationService) {
        this.chatClientBuilder = chatClientBuilder;
        this.heroSearchTool = heroSearchTool;
        this.arenaManagementTool = arenaManagementTool;
        this.squadValidationService = squadValidationService;
        this.todoWriteTool = TodoWriteTool.builder().build();
    }

    public SquadRecommendation buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId) {
        String conversationId = "%s:%s:%d".formatted(teamId, sessionId, roundNo);

        ChatClient chatClient = chatClientBuilder.clone()
                .defaultTools(heroSearchTool, arenaManagementTool, todoWriteTool)
                .defaultAdvisors(
                        ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build())
                                .order(Ordered.HIGHEST_PRECEDENCE + 1000)
                                .build())
                .build();

        SquadRecommendation recommendation = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        Optimize a battle squad for team %s.
                        Session: %s
                        Round: %d

                        Use the TodoWriteTool checklist to keep the optimization steps visible while you work.
                        """.formatted(teamId, sessionId, roundNo))
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(SquadRecommendation.class);

        int validatedTotalCost = squadValidationService.validateAndCalculateTotalCost(sessionId, roundNo, recommendation);
        return new SquadRecommendation(recommendation.heroes(), recommendation.strategy(), recommendation.reasoning(),
                validatedTotalCost);
    }
}

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

import java.util.Comparator;
import java.util.List;
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

    private static final List<DraftingStrategy> STRATEGIES = List.of(
            new DraftingStrategy(
                    "balanced-drafter",
                    "Balanced Draft",
                    "General-purpose, low-risk composition.",
                    """
                            Draft a balanced, low-risk squad.
                            Prioritize role coverage first, then fit under budget, then general quality.
                            Avoid fragile tradeoffs unless they materially improve compliance.
                            """),
            new DraftingStrategy(
                    "budget-drafter",
                    "Budget Saver",
                    "Minimize spend while staying valid.",
                    """
                            Draft the cheapest valid squad you can assemble.
                            Lock required roles first, preserve budget headroom, and prefer lower-cost substitutes.
                            Reason explicitly about spare budget remaining after the draft.
                            """),
            new DraftingStrategy(
                    "synergy-drafter",
                    "Synergy Draft",
                    "Lean into tags and role interactions allowed by the round.",
                    """
                            Draft a synergy-focused squad.
                            Favor complementary tags, coherent role interactions, and round modifiers when available.
                            If synergy conflicts with hard constraints, satisfy the constraints first.
                            """),
            new DraftingStrategy(
                    "aggressive-drafter",
                    "Aggressive Draft",
                    "Bias toward offensive pressure and higher-impact picks.",
                    """
                            Draft an aggressive squad.
                            Prioritize offensive pressure and high-impact picks while still staying fully valid.
                            If you must trade off between aggression and legality, legality wins.
                            """));

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

    public DraftOptionsResponse buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId) {
        List<DraftOption> options = STRATEGIES.stream()
                .map(strategy -> buildDraftOption(teamId, roundNo, sessionId, strategy))
                .toList();

        DraftOption recommendedOption = options.stream()
                .filter(DraftOption::valid)
                .min(Comparator.comparingInt(option -> option.recommendation().totalCost()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No drafting strategy produced a valid squad for this round."));

        List<DraftOption> finalizedOptions = options.stream()
                .map(option -> option.strategyId().equals(recommendedOption.strategyId())
                        ? new DraftOption(option.strategyId(), option.label(), option.summary(), option.recommendation(),
                                true, option.valid(), option.violations())
                        : option)
                .toList();

        return new DraftOptionsResponse(finalizedOptions, recommendedOption.strategyId());
    }

    private DraftOption buildDraftOption(UUID teamId, int roundNo, UUID sessionId, DraftingStrategy strategy) {
        String conversationId = "%s:%s:%d:%s".formatted(teamId, sessionId, roundNo, strategy.id());
        ChatClient chatClient = buildStrategyClient();

        SquadRecommendation recommendation = chatClient.prompt()
                .system(SYSTEM_PROMPT + "\n\nStrategy profile:\n" + strategy.instructions())
                .user("""
                        Optimize a battle squad for team %s.
                        Session: %s
                        Round: %d

                        Strategy profile: %s
                        Use the TodoWriteTool checklist to keep the optimization steps visible while you work.
                        """.formatted(teamId, sessionId, roundNo, strategy.label()))
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(SquadRecommendation.class);

        SquadValidationService.ValidationResult validation = squadValidationService.validateSquad(sessionId, roundNo,
                recommendation);
        SquadRecommendation normalizedRecommendation = new SquadRecommendation(
                recommendation.heroes(),
                recommendation.strategy(),
                recommendation.reasoning(),
                validation.totalCost());

        return new DraftOption(
                strategy.id(),
                strategy.label(),
                strategy.summary(),
                normalizedRecommendation,
                false,
                validation.valid(),
                validation.violations());
    }

    private ChatClient buildStrategyClient() {
        return chatClientBuilder.clone()
                .defaultTools(heroSearchTool, arenaManagementTool, todoWriteTool)
                .defaultAdvisors(
                        ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build())
                                .order(Ordered.HIGHEST_PRECEDENCE + 1000)
                                .build())
                .build();
    }

    private record DraftingStrategy(String id, String label, String summary, String instructions) {
    }
}

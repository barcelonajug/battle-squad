package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public DraftOptionsResponse buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId) {
        // TODO Phase 1: Define the strategy list that the orchestrator will evaluate.
        // Example strategies for the workshop:
        // - balanced-drafter
        // - budget-drafter
        // - synergy-drafter
        // - aggressive-drafter
        List<String> strategies = List.of(
                "balanced-drafter",
                "budget-drafter",
                "synergy-drafter",
                "aggressive-drafter");

        // TODO Phase 2: Configure MessageChatMemoryAdvisor before TodoWriteTool.
        // Scope the memory by teamId + sessionId + roundNo so each optimization run has
        // isolated context.
        // The todo list should be written into that shared memory channel.

        // TODO Phase 3: Register TodoWriteTool so the agent exposes a visible checklist
        // while it drafts the squad.

        // TODO Phase 4: Keep heroSearchTool and arenaManagementTool stateless and
        // narrowly scoped to API access.
        // They should not track progress, memory, or validation state.

        // TODO Phase 5: Create one prompt per drafting strategy.
        // Each strategy should return a SquadRecommendation with reasoning, strategy, and
        // selected heroes.

        // TODO Phase 6: Add deterministic validation after each AI draft.
        // Validate:
        // - exactly the required number of heroes
        // - total cost under the round budget
        // - role requirements satisfied
        // - banned tags and round-specific constraints obeyed

        // TODO Phase 7: Wrap every result into DraftOption and return
        // DraftOptionsResponse.
        // Mark one valid option as recommended so the UI can render a strategy picker.

        // TODO Phase 8: Once this workflow is complete, the controller and UI are
        // already ready to display multiple options.

        throw new UnsupportedOperationException(
                "TODO: Implement the phased drafting workflow with chat memory, TodoWriteTool, and draft options");
    }
}

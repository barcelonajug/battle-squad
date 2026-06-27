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
        // Workshop Step 2: Configure BattleAdvisorService.
        // Define the strategy list that the orchestrator will evaluate.
        // Example strategies:
        // - balanced-drafter
        // - budget-drafter
        // - synergy-drafter
        // - aggressive-drafter
        List<String> strategies = List.of(
                "balanced-drafter",
                "budget-drafter",
                "synergy-drafter",
                "aggressive-drafter");

        // Workshop Step 3: Add chat memory first.
        // Configure MessageChatMemoryAdvisor before TodoWriteTool.
        // Scope the memory by teamId + sessionId + roundNo so each optimization run has
        // isolated context.
        // TodoWriteTool will use that same memory-backed run context.

        // Workshop Step 4: Add TodoWriteTool on top of chat memory.
        // Register TodoWriteTool so the agent exposes a visible checklist while it
        // drafts the squad.

        // Workshop Step 2 also wires the stateless tools into the ChatClient.
        // heroSearchTool and arenaManagementTool should stay narrowly scoped to API
        // access and should not track progress, memory, or validation state.

        // Workshop Step 5: Use round-aware hero search.
        // Create one prompt per drafting strategy.
        // Each strategy should return a SquadRecommendation with reasoning, strategy, and
        // selected heroes.
        // The prompt should require findHeroesForRound before drafting so the agent uses
        // /api/heroes/search/advanced with the round's allowed roles, genders, races,
        // publishers, alignments, and budget.

        // Workshop Step 6: Validate every draft deterministically.
        // Validate:
        // - exactly the required number of heroes
        // - total cost under the round budget
        // - role requirements satisfied
        // - banned tags and round-specific constraints obeyed
        // - allowed roles, genders, races, publishers, and alignments obeyed

        // Workshop Step 7: Expose multiple draft options.
        // Wrap every result into DraftOption and return DraftOptionsResponse.
        // Mark one valid option as recommended so the UI can render a strategy picker.

        // The controller and UI are already prepared to display multiple options once
        // this workflow is implemented.

        throw new UnsupportedOperationException(
                "TODO: Implement the phased drafting workflow with chat memory, TodoWriteTool, and draft options");
    }
}

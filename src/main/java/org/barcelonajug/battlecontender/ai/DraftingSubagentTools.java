package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.barcelonajug.battlecontender.ai.tools.SquadValidationTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.List;
import java.util.Set;

final class DraftingSubagentTools {

    private static final Set<String> ALLOWED_TOOL_NAMES = Set.of(
            "findHeroesForRound",
            "getHeroDetails",
            "getRoundConstraints",
            "validateSquadForRound");

    private DraftingSubagentTools() {
    }

    static List<ToolCallback> callbacks(
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool,
            SquadValidationTool squadValidationTool) {
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(heroSearchTool, arenaManagementTool, squadValidationTool)
                .build()
                .getToolCallbacks();

        return List.of(callbacks).stream()
                .filter(callback -> ALLOWED_TOOL_NAMES.contains(callback.getToolDefinition().name()))
                .toList();
    }
}

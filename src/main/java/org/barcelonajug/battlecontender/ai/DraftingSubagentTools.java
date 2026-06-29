package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.util.List;
import java.util.Set;

final class DraftingSubagentTools {

    private static final Set<String> ALLOWED_TOOL_NAMES = Set.of(
            "findHeroesForRound",
            "getHeroDetails",
            "getRoundConstraints",
            "TodoWrite");

    private DraftingSubagentTools() {
    }

    static List<ToolCallback> callbacks(
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool,
            TodoWriteTool todoWriteTool) {
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(heroSearchTool, arenaManagementTool, todoWriteTool)
                .build()
                .getToolCallbacks();

        return List.of(callbacks).stream()
                .filter(callback -> ALLOWED_TOOL_NAMES.contains(callback.getToolDefinition().name()))
                .toList();
    }
}

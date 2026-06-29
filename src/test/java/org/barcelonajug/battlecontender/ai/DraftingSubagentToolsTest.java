package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.TodoWriteTool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DraftingSubagentToolsTest {

    @Test
    void callbacks_exposeOnlyRoundAwareDraftingTools() {
        var callbacks = DraftingSubagentTools.callbacks(
                mock(HeroSearchTool.class),
                mock(ArenaManagementTool.class),
                TodoWriteTool.builder().build());

        assertThat(callbacks)
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder(
                        "findHeroesForRound",
                        "getHeroDetails",
                        "getRoundConstraints",
                        "TodoWrite");
    }
}

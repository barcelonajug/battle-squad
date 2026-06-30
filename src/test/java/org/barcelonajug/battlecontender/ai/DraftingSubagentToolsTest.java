package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.barcelonajug.battlecontender.ai.tools.SquadValidationTool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DraftingSubagentToolsTest {

    @Test
    void callbacks_exposeOnlyRoundAwareDraftingTools() {
        var callbacks = DraftingSubagentTools.callbacks(
                mock(HeroSearchTool.class),
                mock(ArenaManagementTool.class),
                mock(SquadValidationTool.class));

        assertThat(callbacks)
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder(
                        "findHeroesForRound",
                        "getHeroDetails",
                        "getRoundConstraints",
                        "validateSquadForRound");
    }
}

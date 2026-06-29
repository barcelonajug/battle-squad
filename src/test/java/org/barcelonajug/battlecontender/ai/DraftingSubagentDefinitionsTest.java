package org.barcelonajug.battlecontender.ai;

import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.task.subagent.claude.ClaudeSubagentDefinition;
import org.springaicommunity.agent.tools.task.subagent.claude.ClaudeSubagentReferences;
import org.springaicommunity.agent.tools.task.subagent.claude.ClaudeSubagentResolver;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DraftingSubagentDefinitionsTest {

    private static final Set<String> EXPECTED_TOOLS = Set.of(
            "findHeroesForRound",
            "getHeroDetails",
            "getRoundConstraints",
            "TodoWrite");

    @Test
    void markdownDefinitions_registerFourMiniModelDraftersWithRestrictedTools() throws IOException {
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath:/agents/*.md");
        var references = ClaudeSubagentReferences.fromResources(resources);
        var resolver = new ClaudeSubagentResolver();

        var definitions = references.stream()
                .map(resolver::resolve)
                .map(ClaudeSubagentDefinition.class::cast)
                .toList();

        assertThat(definitions).hasSize(4);
        assertThat(definitions.stream().map(ClaudeSubagentDefinition::getName).collect(Collectors.toSet()))
                .isEqualTo(DraftingStrategies.ALL.stream()
                        .map(DraftingStrategies.DraftingStrategy::id)
                        .collect(Collectors.toSet()));
        assertThat(definitions).allSatisfy(definition -> {
            assertThat(definition.getModel()).isEqualTo("mini");
            assertThat(definition.tools()).containsExactlyInAnyOrderElementsOf(EXPECTED_TOOLS);
            assertThat(definition.getDescription()).isNotBlank();
        });
    }
}

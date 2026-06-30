package org.barcelonajug.battlecontender.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiDraftingSubagentOrchestratorTest {

    @Test
    void draft_returnsResultsCollectedByTheParentAgent() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        SquadRecommendation recommendation = new SquadRecommendation(
                List.of(new SquadRecommendation.HeroRef(1, "Superman")),
                "Balanced",
                "Valid squad",
                10);
        SubagentDraftResponse response = new SubagentDraftResponse(
                List.of(new SubagentDraft("balanced-drafter", recommendation, null)));
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.<String>argThat(
                        prompt -> prompt.contains("run_in_background=true")
                        && prompt.contains("balanced-drafter")
                        && prompt.contains("budget-drafter")
                        && prompt.contains("synergy-drafter")
                        && prompt.contains("aggressive-drafter")
                        && prompt.contains("TaskOutput with block=true")))
                .user(org.mockito.ArgumentMatchers.<String>argThat(
                        prompt -> prompt.contains("Launch all four specialized drafting subagents in parallel")))
                .advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())
                .call()
                .entity(SubagentDraftResponse.class)).thenReturn(response);

        SpringAiDraftingSubagentOrchestrator orchestrator = new SpringAiDraftingSubagentOrchestrator(chatClient);

        List<SubagentDraft> actual = orchestrator.draft(UUID.randomUUID(), UUID.randomUUID(), 3);

        assertThat(actual).containsExactlyElementsOf(response.drafts());
    }
}

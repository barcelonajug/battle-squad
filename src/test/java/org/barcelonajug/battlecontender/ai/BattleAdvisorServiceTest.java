package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleAdvisorServiceTest {

    @Mock(answer = Answers.RETURNS_SELF)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private HeroSearchTool heroSearchTool;

    @Mock
    private ArenaManagementTool arenaManagementTool;

    @Test
    void buildOptimalSquad_returnsStructuredRecommendation() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        SquadRecommendation expected = new SquadRecommendation(
                List.of(new SquadRecommendation.HeroRef(1, "Superman")),
                "Lean on a durable tank and keep the team under budget.",
                "Checklist: load constraints, compare candidates, lock the best fit.",
                15);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .advisors(org.mockito.ArgumentMatchers.any())
                .call()
                .entity(SquadRecommendation.class)).thenReturn(expected);

        BattleAdvisorService service = new BattleAdvisorService(chatClientBuilder, heroSearchTool, arenaManagementTool);

        SquadRecommendation actual = service.buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(actual).isEqualTo(expected);
    }
}

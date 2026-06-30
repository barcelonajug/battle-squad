package org.barcelonajug.battlecontender.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleAdvisorServiceTest {

    @Mock
    private DeterministicSquadSolver deterministicSquadSolver;

    @Test
    void buildOptimalSquad_delegatesToDeterministicSolver() {
        UUID teamId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        SquadRecommendation recommendation = new SquadRecommendation(
                List.of(new SquadRecommendation.HeroRef(1, "Superman")),
                "Balanced Draft",
                "Generated deterministically.",
                15);
        DraftOption option = new DraftOption(
                "balanced-drafter", "Balanced Draft", "Balanced", recommendation, true, true, List.of());
        DraftOptionsResponse expected = new DraftOptionsResponse(List.of(option), "balanced-drafter");
        when(deterministicSquadSolver.solve(sessionId, 2)).thenReturn(expected);
        BattleAdvisorService service = new BattleAdvisorService(deterministicSquadSolver);

        DraftOptionsResponse actual = service.buildOptimalSquad(teamId, 2, sessionId);

        assertThat(actual).isEqualTo(expected);
        verify(deterministicSquadSolver).solve(sessionId, 2);
    }
}

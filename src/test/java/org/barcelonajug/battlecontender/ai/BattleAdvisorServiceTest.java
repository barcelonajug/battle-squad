package org.barcelonajug.battlecontender.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleAdvisorServiceTest {

    @Mock
    private DraftingSubagentOrchestrator draftingSubagentOrchestrator;

    @Mock
    private SquadValidationService squadValidationService;

    @Test
    void buildOptimalSquad_returnsValidatedDraftOptions() {
        SquadRecommendation expected = recommendation();
        when(draftingSubagentOrchestrator.draft(any(), any(), anyInt()))
                .thenReturn(DraftingStrategies.ALL.stream()
                        .map(strategy -> new SubagentDraft(strategy.id(), expected, null))
                        .toList());
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 15, List.of()));

        BattleAdvisorService service = new BattleAdvisorService(draftingSubagentOrchestrator, squadValidationService);

        DraftOptionsResponse actual = service.buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(actual.options()).hasSize(4);
        assertThat(actual.recommendedStrategyId()).isEqualTo("balanced-drafter");
        assertThat(actual.options()).allMatch(DraftOption::valid);
        assertThat(actual.options()).filteredOn(DraftOption::recommended).hasSize(1);
    }

    @Test
    void buildOptimalSquad_preservesMissingDraftAsInvalidOption() {
        List<SubagentDraft> drafts = DraftingStrategies.ALL.stream()
                .limit(3)
                .map(strategy -> new SubagentDraft(strategy.id(), recommendation(), null))
                .toList();
        when(draftingSubagentOrchestrator.draft(any(), any(), anyInt())).thenReturn(drafts);
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 15, List.of()));

        DraftOptionsResponse actual = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(actual.options()).hasSize(4);
        assertThat(actual.options()).filteredOn(option -> option.strategyId().equals("aggressive-drafter"))
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.valid()).isFalse();
                    assertThat(option.violations()).containsExactly("Subagent did not return a draft.");
                });
    }

    @Test
    void buildOptimalSquad_doesNotRetryMalformedDraft() {
        List<SubagentDraft> drafts = DraftingStrategies.ALL.stream()
                .map(strategy -> strategy.id().equals("budget-drafter")
                        ? new SubagentDraft(strategy.id(), null, "Budget drafter returned invalid JSON.")
                        : new SubagentDraft(strategy.id(), recommendation(), null))
                .toList();
        when(draftingSubagentOrchestrator.draft(any(), any(), anyInt())).thenReturn(drafts);
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 15, List.of()));

        DraftOptionsResponse actual = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(actual.options()).filteredOn(option -> option.strategyId().equals("budget-drafter"))
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.valid()).isFalse();
                    assertThat(option.violations()).containsExactly("Budget drafter returned invalid JSON.");
                });
        verify(draftingSubagentOrchestrator, times(1)).draft(any(), any(), anyInt());
        verify(squadValidationService, times(3))
                .validateSquad(any(), anyInt(), any(SquadRecommendation.class));
    }

    @Test
    void buildOptimalSquad_marksDuplicateStrategyAsInvalid() {
        List<SubagentDraft> drafts = new java.util.ArrayList<>(DraftingStrategies.ALL.stream()
                .map(strategy -> new SubagentDraft(strategy.id(), recommendation(), null))
                .toList());
        drafts.add(new SubagentDraft("balanced-drafter", recommendation(), null));
        when(draftingSubagentOrchestrator.draft(any(), any(), anyInt())).thenReturn(drafts);
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 15, List.of()));

        DraftOptionsResponse actual = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(actual.options()).filteredOn(option -> option.strategyId().equals("balanced-drafter"))
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.valid()).isFalse();
                    assertThat(option.violations())
                            .containsExactly("Subagent returned 2 drafts; expected exactly one.");
                });
    }

    @Test
    void buildOptimalSquad_returnsInvalidOptionsWhenNoSubagentProducesAValidDraft() {
        when(draftingSubagentOrchestrator.draft(any(), any(), anyInt()))
                .thenReturn(DraftingStrategies.ALL.stream()
                        .map(strategy -> new SubagentDraft(strategy.id(), recommendation(), null))
                        .toList());
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(
                        false,
                        15,
                        List.of("Expected exactly 5 heroes but got 1.")));

        DraftOptionsResponse actual = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(actual.options()).hasSize(4).allMatch(option -> !option.valid());
        assertThat(actual.options()).allSatisfy(option -> {
            assertThat(option.recommendation().heroes()).hasSize(1);
            assertThat(option.violations()).containsExactly("Expected exactly 5 heroes but got 1.");
        });
        assertThat(actual.recommendedStrategyId()).isNull();
        assertThat(actual.options()).noneMatch(DraftOption::recommended);
    }

    private BattleAdvisorService service() {
        return new BattleAdvisorService(draftingSubagentOrchestrator, squadValidationService);
    }

    private SquadRecommendation recommendation() {
        return new SquadRecommendation(
                List.of(new SquadRecommendation.HeroRef(1, "Superman")),
                "Lean on a durable tank and keep the team under budget.",
                "Checklist: load constraints, compare candidates, lock the best fit.",
                15);
    }
}

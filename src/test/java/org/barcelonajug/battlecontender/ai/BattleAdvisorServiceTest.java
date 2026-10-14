package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Team;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.argThat;

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

    @Mock
    private SquadValidationService squadValidationService;

    @Mock
    private SquadEvaluator squadEvaluator;

    @Mock
    private ArenaApiClient arenaApiClient;

    @Test
    void buildOptimalSquad_returnsValidatedDraftOptions() {
        when(chatClientBuilder.clone()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        SquadRecommendation expected = new SquadRecommendation(
                List.of(new SquadRecommendation.HeroRef(1, "Superman")),
                "Lean on a durable tank and keep the team under budget.",
                "Checklist: load constraints, compare candidates, lock the best fit.",
                15);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())
                .call()
                .entity(SquadRecommendation.class)).thenReturn(expected);
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 15, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenThrow(new IllegalStateException("Jev unavailable"));

        BattleAdvisorService service = new BattleAdvisorService(chatClientBuilder, heroSearchTool, arenaManagementTool,
                squadValidationService, squadEvaluator, arenaApiClient, Runnable::run);

        List<BattleAdvisorService.OptimizationProgress> progress = new ArrayList<>();
        DraftOptionsResponse actual = service.buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID(), progress::add);

        assertThat(actual.options()).hasSize(4);
        assertThat(actual.recommendedStrategyId()).isEqualTo("aggressive-drafter");
        assertThat(actual.options()).allMatch(DraftOption::valid);
        assertThat(actual.options()).filteredOn(DraftOption::recommended).hasSize(1);
        assertThat(actual.selectionMethod()).isEqualTo("COST_FALLBACK");
        assertThat(actual.options()).allMatch(option -> "UNAVAILABLE".equals(option.evaluationStatus()));
        assertThat(progress).hasSize(8);
        assertThat(progress.getLast()).isEqualTo(new BattleAdvisorService.OptimizationProgress(4, 4, 4));
    }

    @Test
    void draftingStrategiesStartConcurrently() throws Exception {
        stubDrafts();
        CountDownLatch started = new CountDownLatch(4);
        CountDownLatch release = new CountDownLatch(1);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())
                .call()
                .entity(SquadRecommendation.class)).thenAnswer(invocation -> {
                    started.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Draft was blocked");
                    return new SquadRecommendation(List.of(new SquadRecommendation.HeroRef(1, "Hero")),
                            "Strategy", "Reason", 0);
                });
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 15, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenReturn(new SquadEvaluator.Evaluation(80, 0.9));
        BattleAdvisorService service = new BattleAdvisorService(chatClientBuilder, heroSearchTool, arenaManagementTool,
                squadValidationService, squadEvaluator, arenaApiClient, command -> Thread.startVirtualThread(command));

        CompletableFuture<DraftOptionsResponse> result = CompletableFuture.supplyAsync(() ->
                service.buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID()));
        try {
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
        }
        assertThat(result.get(5, TimeUnit.SECONDS).options()).hasSize(4);
    }

    @Test
    void draftingFailurePreservesItsOriginalError() {
        stubDrafts();
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenThrow(new IllegalArgumentException("Round is unavailable"));

        assertThatThrownBy(() -> service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Round is unavailable");
    }

    @Test
    void oneMalformedDraftDoesNotDiscardOtherValidOptions() {
        stubDrafts();
        SquadRecommendation recommendation = new SquadRecommendation(
                List.of(new SquadRecommendation.HeroRef(1, "Hero")), "Strategy", "Reason", 0);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())
                .call()
                .entity(SquadRecommendation.class))
                .thenReturn(recommendation)
                .thenThrow(new IllegalStateException("Malformed structured output"))
                .thenReturn(recommendation)
                .thenReturn(recommendation);
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 15, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenReturn(new SquadEvaluator.Evaluation(80, 0.9));
        List<BattleAdvisorService.OptimizationProgress> progress = new ArrayList<>();

        DraftOptionsResponse result = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID(),
                progress::add);

        assertThat(result.options()).hasSize(4);
        assertThat(result.options().get(1).evaluationStatus()).isEqualTo("DRAFT_FAILED");
        assertThat(result.options().get(1).valid()).isFalse();
        assertThat(result.options()).filteredOn(DraftOption::valid).hasSize(3);
        assertThat(result.selectionMethod()).isEqualTo("JEV");
        assertThat(progress.getLast()).isEqualTo(new BattleAdvisorService.OptimizationProgress(4, 4, 4));
        verify(squadEvaluator, times(3)).evaluate(any(), anyInt(), any(DraftOption.class));
    }

    @Test
    void validModelDraftOutsideTeamSampleUsesValidatedTeamAlternative() {
        stubDrafts();
        UUID sessionId = UUID.randomUUID();
        HeroSearchTool.HeroSummary candidate = new HeroSearchTool.HeroSummary(
                2, "Team Hero", "Fighter", 12, "good", "Marvel", "Female", "Human", List.of());
        when(heroSearchTool.findHeroesForTeamRound(any(), any(), anyInt(), any()))
                .thenReturn(List.of(candidate));
        when(heroSearchTool.buildTeamBaseline(any(), any(), anyInt(), any()))
                .thenReturn(List.of(candidate));
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenAnswer(invocation -> {
                    SquadRecommendation draft = invocation.getArgument(2);
                    return new SquadValidationService.ValidationResult(true,
                            draft.heroes().getFirst().id() == 2 ? 12 : 15, List.of());
                });
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenReturn(new SquadEvaluator.Evaluation(80, 0.9));

        DraftOptionsResponse result = service().buildOptimalSquad(UUID.randomUUID(), 1, sessionId);

        assertThat(result.options()).allMatch(DraftOption::valid);
        assertThat(result.options()).allMatch(option -> option.recommendation().heroes().getFirst().id() == 2);
        assertThat(result.options()).allMatch(option -> option.recommendation().totalCost() == 12);
        verify(squadValidationService, times(8)).validateSquad(any(), anyInt(), any(SquadRecommendation.class));
    }

    @Test
    void stalledDraftsReturnWithinTheDeadline() {
        stubDrafts();
        CountDownLatch release = new CountDownLatch(1);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())
                .call()
                .entity(SquadRecommendation.class)).thenAnswer(invocation -> {
                    release.await(5, TimeUnit.SECONDS);
                    return new SquadRecommendation(List.of(), "Strategy", "Reason", 0);
                });
        BattleAdvisorService service = new BattleAdvisorService(chatClientBuilder, heroSearchTool, arenaManagementTool,
                squadValidationService, squadEvaluator, arenaApiClient, command -> Thread.startVirtualThread(command), 50, 50);

        try {
            assertThatThrownBy(() -> service.buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(java.util.concurrent.TimeoutException.class);
        } finally {
            release.countDown();
        }
    }

    @Test
    void buildOptimalSquad_ranksOnlyValidOptionsByJevScore() {
        stubDrafts();
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(false, 10, List.of("Invalid")))
                .thenReturn(new SquadValidationService.ValidationResult(true, 30, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 20, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 40, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenAnswer(invocation -> switch (((DraftOption) invocation.getArgument(2)).strategyId()) {
                    case "budget-drafter" -> new SquadEvaluator.Evaluation(55, 0.8);
                    case "synergy-drafter" -> new SquadEvaluator.Evaluation(85, 0.9);
                    default -> new SquadEvaluator.Evaluation(75, 0.7);
                });

        DraftOptionsResponse result = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(result.recommendedStrategyId()).isEqualTo("synergy-drafter");
        assertThat(result.selectionMethod()).isEqualTo("JEV");
        assertThat(result.options().get(2).evaluationScore()).isEqualTo(85);
        assertThat(result.options().get(2).evaluationConfidence()).isEqualTo(0.9);
        assertThat(result.options().getFirst().evaluationStatus()).isEqualTo("NOT_EVALUATED");
        verify(squadEvaluator, times(3)).evaluate(any(), anyInt(), any(DraftOption.class));
        verify(squadEvaluator, never()).evaluate(any(), anyInt(),
                argThat(option -> "balanced-drafter".equals(option.strategyId())));
    }

    @Test
    void buildOptimalSquad_ranksConfidentOptionsDespiteAnotherLowConfidenceEvaluation() {
        stubDrafts();
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 20, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 10, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 30, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 40, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenAnswer(invocation -> switch (((DraftOption) invocation.getArgument(2)).strategyId()) {
                    case "balanced-drafter" -> new SquadEvaluator.Evaluation(95, 0.2);
                    case "synergy-drafter" -> new SquadEvaluator.Evaluation(85, 0.8);
                    default -> new SquadEvaluator.Evaluation(70, 0.8);
                });

        DraftOptionsResponse result = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(result.recommendedStrategyId()).isEqualTo("synergy-drafter");
        assertThat(result.selectionMethod()).isEqualTo("JEV");
        assertThat(result.options().getFirst().evaluationStatus()).isEqualTo("LOW_CONFIDENCE");
    }

    @Test
    void buildOptimalSquad_fallsBackOnlyWhenNoEvaluationIsUsable() {
        stubDrafts();
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 20, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 10, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 30, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 40, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenAnswer(invocation -> "balanced-drafter".equals(((DraftOption) invocation.getArgument(2)).strategyId())
                        ? new SquadEvaluator.Evaluation(95, 0.2)
                        : new SquadEvaluator.Evaluation(70, 0.1));

        DraftOptionsResponse result = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(result.recommendedStrategyId()).isEqualTo("budget-drafter");
        assertThat(result.selectionMethod()).isEqualTo("COST_FALLBACK");
        assertThat(result.options()).allMatch(option -> "LOW_CONFIDENCE".equals(option.evaluationStatus()));
    }

    @Test
    void buildOptimalSquad_ignoresUnavailableAndLowConfidenceOptionsWhenRankingTies() {
        stubDrafts();
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 5, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 10, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 20, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 20, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenAnswer(invocation -> switch (((DraftOption) invocation.getArgument(2)).strategyId()) {
                    case "balanced-drafter" -> throw new IllegalStateException("Jev unavailable");
                    case "budget-drafter" -> new SquadEvaluator.Evaluation(99, 0.2);
                    default -> new SquadEvaluator.Evaluation(80, 0.8);
                });

        DraftOptionsResponse result = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(result.recommendedStrategyId()).isEqualTo("aggressive-drafter");
        assertThat(result.selectionMethod()).isEqualTo("JEV");
        assertThat(result.options()).extracting(DraftOption::evaluationStatus)
                .containsExactly("UNAVAILABLE", "LOW_CONFIDENCE", "EVALUATED", "EVALUATED");
    }

    @Test
    void buildOptimalSquad_breaksJevScoreTiesByCostThenStrategyId() {
        stubDrafts();
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 30, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 20, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 20, List.of()))
                .thenReturn(new SquadValidationService.ValidationResult(true, 40, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenReturn(new SquadEvaluator.Evaluation(80, 0.8));

        DraftOptionsResponse result = service().buildOptimalSquad(UUID.randomUUID(), 2, UUID.randomUUID());

        assertThat(result.recommendedStrategyId()).isEqualTo("budget-drafter");
        assertThat(result.selectionMethod()).isEqualTo("JEV");
    }

    @Test
    void registeredTeamsPreferDistinctValidStrategiesAcrossTheSession() {
        stubDrafts();
        UUID sessionId = UUID.randomUUID();
        LocalDateTime registeredAt = LocalDateTime.now();
        List<Team> teams = java.util.stream.IntStream.range(0, 4)
                .mapToObj(index -> new Team(UUID.randomUUID(), sessionId, "Client " + index,
                        registeredAt.plusSeconds(index), List.of("Codex")))
                .toList();
        when(arenaApiClient.listTeams(sessionId)).thenReturn(teams);
        when(squadValidationService.validateSquad(any(), anyInt(), any(SquadRecommendation.class)))
                .thenReturn(new SquadValidationService.ValidationResult(true, 30, List.of()));
        when(squadEvaluator.evaluate(any(), anyInt(), any(DraftOption.class)))
                .thenReturn(new SquadEvaluator.Evaluation(80, 0.2));

        List<DraftOptionsResponse> results = teams.stream()
                .map(team -> service().buildOptimalSquad(team.teamId(), 2, sessionId))
                .toList();

        assertThat(results).extracting(DraftOptionsResponse::recommendedStrategyId)
                .containsExactly("balanced-drafter", "budget-drafter", "synergy-drafter", "aggressive-drafter");
        assertThat(results).allMatch(result -> "TEAM_STRATEGY".equals(result.selectionMethod()));
    }

    private void stubDrafts() {
        when(chatClientBuilder.clone()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()
                .system(org.mockito.ArgumentMatchers.anyString())
                .user(org.mockito.ArgumentMatchers.anyString())
                .advisors(org.mockito.ArgumentMatchers.<Consumer<ChatClient.AdvisorSpec>>any())
                .call()
                .entity(SquadRecommendation.class)).thenReturn(new SquadRecommendation(
                        List.of(new SquadRecommendation.HeroRef(1, "Superman")), "Strategy", "Reasoning", 0));
    }

    private BattleAdvisorService service() {
        return new BattleAdvisorService(chatClientBuilder, heroSearchTool, arenaManagementTool,
                squadValidationService, squadEvaluator, arenaApiClient, Runnable::run);
    }
}

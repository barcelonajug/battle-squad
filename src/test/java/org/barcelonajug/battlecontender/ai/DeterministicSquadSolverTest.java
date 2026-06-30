package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Appearance;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.PowerStats;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeterministicSquadSolverTest {

    @Test
    void solve_returnsDistinctValidDraftsForEveryStrategy() {
        ArenaApiClient arenaApiClient = mock(ArenaApiClient.class);
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Mixed team",
                2,
                30,
                Map.of("Fighter", 1, "Support", 1),
                Map.of("Fighter", 1, "Support", 1),
                List.of("banned"),
                Map.of("magic", 1.5, "tech", 1.2),
                "ARENA_1",
                List.of("Fighter", "Support"),
                List.of("Female"),
                List.of(),
                List.of(),
                List.of("good"));
        List<Hero> heroes = List.of(
                hero(1, "Fighter One", "Fighter", 8, "magic", 80, 75, 60, 70, 50, 85),
                hero(2, "Fighter Two", "Fighter", 11, "tech", 90, 85, 70, 80, 40, 95),
                hero(3, "Fighter Three", "Fighter", 6, "cosmic", 55, 60, 50, 65, 70, 60),
                hero(4, "Support One", "Support", 7, "magic", 45, 35, 90, 55, 95, 40),
                hero(5, "Support Two", "Support", 10, "tech", 60, 45, 75, 65, 85, 55),
                hero(6, "Support Three", "Support", 5, "cosmic", 35, 30, 55, 45, 70, 35));
        when(arenaApiClient.getRound(2, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.listHeroes(0, 200)).thenReturn(heroes);
        heroes.forEach(hero -> when(arenaApiClient.getHero(hero.id())).thenReturn(hero));

        DeterministicSquadSolver solver = new DeterministicSquadSolver(
                arenaApiClient,
                new SquadValidationService(arenaApiClient));

        DraftOptionsResponse response = solver.solve(sessionId, 2);

        assertThat(response.options()).hasSize(4).allMatch(DraftOption::valid);
        assertThat(response.options()).extracting(DraftOption::strategyId)
                .containsExactly("balanced-drafter", "budget-drafter", "synergy-drafter", "aggressive-drafter");
        Set<Set<Integer>> squads = response.options().stream()
                .map(option -> option.recommendation().heroes().stream()
                        .map(SquadRecommendation.HeroRef::id)
                        .collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        assertThat(squads).hasSize(4);
        assertThat(response.recommendedStrategyId()).isNotBlank();
        assertThat(response.options()).filteredOn(DraftOption::recommended).hasSize(1);
        assertThat(solver.solve(sessionId, 2)).isEqualTo(response);
    }

    @Test
    void solve_returnsInvalidOptionsWhenNoSquadCanMeetTheRound() {
        ArenaApiClient arenaApiClient = mock(ArenaApiClient.class);
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Impossible round",
                2,
                10,
                Map.of("Fighter", 2),
                Map.of(),
                List.of(),
                Map.of(),
                "ARENA_1");
        Hero onlyHero = hero(1, "Only Fighter", "Fighter", 5, "magic", 50, 50, 50, 50, 50, 50);
        when(arenaApiClient.getRound(1, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.listHeroes(0, 200)).thenReturn(List.of(onlyHero));

        DeterministicSquadSolver solver = new DeterministicSquadSolver(
                arenaApiClient,
                new SquadValidationService(arenaApiClient));

        DraftOptionsResponse response = solver.solve(sessionId, 1);

        assertThat(response.options()).hasSize(4).allMatch(option -> !option.valid());
        assertThat(response.recommendedStrategyId()).isNull();
        assertThat(response.options()).noneMatch(DraftOption::recommended);
    }

    @Test
    void solve_loadsEligibleHeroesFromLaterCatalogPages() {
        ArenaApiClient arenaApiClient = mock(ArenaApiClient.class);
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Paginated round",
                1,
                20,
                Map.of(),
                Map.of(),
                List.of("banned"),
                Map.of(),
                "ARENA_1");
        List<Hero> bannedFirstPage = IntStream.range(0, 200)
                .mapToObj(index -> hero(
                        1_000 + index, "Banned " + index, "Fighter", 5, "banned", 50, 50, 50, 50, 50, 50))
                .toList();
        List<Hero> eligibleSecondPage = List.of(
                hero(1, "One", "Fighter", 5, "magic", 50, 50, 50, 50, 50, 50),
                hero(2, "Two", "Support", 6, "tech", 60, 60, 60, 60, 60, 60),
                hero(3, "Three", "Fighter", 7, "cosmic", 70, 70, 70, 70, 70, 70),
                hero(4, "Four", "Support", 8, "nature", 80, 80, 80, 80, 80, 80));
        when(arenaApiClient.getRound(3, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.listHeroes(0, 200)).thenReturn(bannedFirstPage);
        when(arenaApiClient.listHeroes(1, 200)).thenReturn(eligibleSecondPage);
        eligibleSecondPage.forEach(hero -> when(arenaApiClient.getHero(hero.id())).thenReturn(hero));

        DeterministicSquadSolver solver = new DeterministicSquadSolver(
                arenaApiClient,
                new SquadValidationService(arenaApiClient));

        DraftOptionsResponse response = solver.solve(sessionId, 3);

        assertThat(response.options()).hasSize(4).allMatch(DraftOption::valid);
        assertThat(response.options())
                .allSatisfy(option -> assertThat(option.recommendation().heroes().getFirst().id()).isLessThan(1_000));
    }

    @Test
    void solve_neverMarksLocallyGeneratedSquadsValidWhenFinalValidationRejectsThem() {
        ArenaApiClient arenaApiClient = mock(ArenaApiClient.class);
        SquadValidationService validationService = mock(SquadValidationService.class);
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Final validation",
                1,
                20,
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "ARENA_1");
        List<Hero> heroes = List.of(
                hero(1, "One", "Fighter", 5, "magic", 50, 50, 50, 50, 50, 50),
                hero(2, "Two", "Support", 6, "tech", 60, 60, 60, 60, 60, 60));
        when(arenaApiClient.getRound(4, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.listHeroes(0, 200)).thenReturn(heroes);
        when(validationService.validateSquad(
                org.mockito.ArgumentMatchers.eq(sessionId),
                org.mockito.ArgumentMatchers.eq(4),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new SquadValidationService.ValidationResult(
                        false, 5, List.of("Arena rejected the squad.")));

        DeterministicSquadSolver solver = new DeterministicSquadSolver(arenaApiClient, validationService);

        DraftOptionsResponse response = solver.solve(sessionId, 4);

        assertThat(response.options()).allMatch(option -> !option.valid());
        assertThat(response.recommendedStrategyId()).isNull();
    }

    @Test
    void solve_breaksEqualStatTiesByCostThenHeroId() {
        ArenaApiClient arenaApiClient = mock(ArenaApiClient.class);
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Tie breaking",
                1,
                20,
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "ARENA_1");
        List<Hero> heroes = List.of(
                hero(2, "Two", "Fighter", 5, "magic", 50, 50, 50, 50, 50, 50),
                hero(1, "One", "Fighter", 5, "magic", 50, 50, 50, 50, 50, 50),
                hero(3, "Three", "Fighter", 6, "magic", 50, 50, 50, 50, 50, 50),
                hero(4, "Four", "Fighter", 7, "magic", 50, 50, 50, 50, 50, 50));
        when(arenaApiClient.getRound(5, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.listHeroes(0, 200)).thenReturn(heroes);
        heroes.forEach(hero -> when(arenaApiClient.getHero(hero.id())).thenReturn(hero));

        DeterministicSquadSolver solver = new DeterministicSquadSolver(
                arenaApiClient,
                new SquadValidationService(arenaApiClient));

        DraftOptionsResponse response = solver.solve(sessionId, 5);

        assertThat(response.options())
                .extracting(option -> option.recommendation().heroes().getFirst().id())
                .containsExactly(1, 2, 3, 4);
    }

    private Hero hero(int id, String name, String role, int cost, String tag,
            int strength, int power, int speed, int durability, int intelligence, int combat) {
        return new Hero(
                id,
                name,
                name.toLowerCase().replace(' ', '-'),
                new PowerStats(durability, strength, power, speed, intelligence, combat),
                role,
                cost,
                "good",
                "BarcelonaJUG",
                new Appearance("Female", "Human", null, null, null, null),
                null,
                List.of(tag),
                null);
    }
}

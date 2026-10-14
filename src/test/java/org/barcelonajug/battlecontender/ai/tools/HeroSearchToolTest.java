package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.AdvancedHeroSearchCriteria;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Appearance;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.PowerStats;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class HeroSearchToolTest {

    @Mock
    private ArenaApiClient arenaApiClient;

    @InjectMocks
    private HeroSearchTool heroSearchTool;

    @Test
    void searchHeroes_mapsUpToTwentyHeroes() {
        when(arenaApiClient.searchHeroes("man")).thenReturn(List.of(
                hero(1, "Superman"),
                hero(2, "Batman"),
                hero(3, "Wonder Woman"),
                hero(4, "Flash"),
                hero(5, "Green Lantern"),
                hero(6, "Aquaman"),
                hero(7, "Martian Manhunter"),
                hero(8, "Cyborg"),
                hero(9, "Hawkman"),
                hero(10, "Zatanna"),
                hero(11, "Shazam"),
                hero(12, "Black Canary"),
                hero(13, "Green Arrow"),
                hero(14, "Batgirl"),
                hero(15, "Nightwing"),
                hero(16, "Robin"),
                hero(17, "Supergirl"),
                hero(18, "Steel"),
                hero(19, "Red Tornado"),
                hero(20, "Hawkgirl"),
                hero(21, "Power Girl")));

        var summaries = heroSearchTool.searchHeroes("man");

        assertThat(summaries).hasSize(20);
        assertThat(summaries.getFirst().name()).isEqualTo("Superman");
        assertThat(summaries.getLast().name()).isEqualTo("Hawkgirl");
        verify(arenaApiClient).searchHeroes("man");
    }

    @Test
    void filterHeroes_mapsSelectedHeroes() {
        when(arenaApiClient.filterHeroes("good", "Marvel Comics")).thenReturn(List.of(
                hero(101, "Spider-Man"),
                hero(102, "Captain Marvel")));

        var summaries = heroSearchTool.filterHeroes("good", "Marvel Comics");

        assertThat(summaries).extracting(HeroSearchTool.HeroSummary::name)
                .containsExactly("Spider-Man", "Captain Marvel");
        verify(arenaApiClient).filterHeroes("good", "Marvel Comics");
    }

    @Test
    void getHeroDetails_returnsApiHero() {
        Hero hero = hero(42, "Doctor Strange");
        when(arenaApiClient.getHero(42)).thenReturn(hero);

        assertThat(heroSearchTool.getHeroDetails(42)).isEqualTo(hero);
        verify(arenaApiClient).getHero(42);
    }

    @Test
    void advancedSearchHeroes_mapsFilteredResults() {
        var criteria = AdvancedHeroSearchCriteria.builder()
                .name("spider")
                .alignment("good")
                .publisher("Marvel Comics")
                .role("Fighter")
                .gender("Male")
                .race("Human")
                .maxCost(25)
                .page(0)
                .size(20)
                .sortBy("cost")
                .sortDirection("ASC")
                .build();
        when(arenaApiClient.advancedSearchHeroes(criteria)).thenReturn(List.of(hero(101, "Spider-Man")));

        var summaries = heroSearchTool.advancedSearchHeroes(
                "spider", "good", "Marvel Comics", "Fighter", "Male", "Human", 25, "cost", "ASC");

        assertThat(summaries).extracting(HeroSearchTool.HeroSummary::name).containsExactly("Spider-Man");
        verify(arenaApiClient).advancedSearchHeroes(criteria);
    }

    @Test
    void findHeroesForRound_appliesAllowedRoundFilters() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Restricted round",
                2,
                50,
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "city",
                List.of("Fighter"),
                List.of("Male"),
                List.of("Human"),
                List.of("Marvel Comics"),
                List.of("good"));
        var criteria = AdvancedHeroSearchCriteria.builder()
                .alignment("good")
                .publisher("Marvel Comics")
                .role("Fighter")
                .gender("Male")
                .race("Human")
                .maxCost(50)
                .page(0)
                .size(20)
                .sortBy("cost")
                .sortDirection("ASC")
                .build();
        when(arenaApiClient.getRound(3, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.advancedSearchHeroes(criteria)).thenReturn(List.of(hero(101, "Spider-Man")));

        var summaries = heroSearchTool.findHeroesForRound(sessionId.toString(), 3, null, null, null, "cost");

        assertThat(summaries).extracting(HeroSearchTool.HeroSummary::name).containsExactly("Spider-Man");
        verify(arenaApiClient).advancedSearchHeroes(criteria);
    }

    @Test
    void findHeroesForRound_acceptsUnrestrictedRound() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec("Open round", 5, 100, Map.of(), Map.of(), List.of(), Map.of(),
                "city", List.of(), List.of(), List.of(), List.of(), List.of());
        var criteria = AdvancedHeroSearchCriteria.builder()
                .maxCost(100)
                .page(0)
                .size(20)
                .sortBy("cost")
                .sortDirection("ASC")
                .build();
        when(arenaApiClient.getRound(1, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.advancedSearchHeroes(criteria)).thenReturn(List.of(hero(101, "Spider-Man")));

        var summaries = heroSearchTool.findHeroesForRound(sessionId.toString(), 1, null, null, null, "cost");

        assertThat(summaries).extracting(HeroSearchTool.HeroSummary::name).containsExactly("Spider-Man");
        verify(arenaApiClient).advancedSearchHeroes(criteria);
    }

    @Test
    void teamRoundSearchSamplesBeyondFirstPageAndVariesByTeam() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec round = new RoundSpec("Open round", 5, 250, Map.of(), Map.of(), List.of(), Map.of(),
                "arena", List.of(), List.of(), List.of(), List.of(), List.of());
        List<Hero> firstPage = IntStream.rangeClosed(1, 100).mapToObj(id -> hero(id, "Hero " + id)).toList();
        List<Hero> secondPage = IntStream.rangeClosed(101, 140).mapToObj(id -> hero(id, "Hero " + id)).toList();
        when(arenaApiClient.getRound(1, sessionId)).thenReturn(round);
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(250).page(0).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(firstPage);
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(250).page(1).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(secondPage);

        List<HeroSearchTool.HeroSummary> firstTeam = heroSearchTool.findHeroesForTeamRound(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), sessionId, 1, "balanced-drafter");
        List<HeroSearchTool.HeroSummary> secondTeam = heroSearchTool.findHeroesForTeamRound(
                UUID.fromString("00000000-0000-0000-0000-000000000002"), sessionId, 1, "balanced-drafter");

        assertThat(firstTeam).hasSize(60);
        assertThat(secondTeam).hasSize(60);
        assertThat(firstTeam).anyMatch(candidate -> candidate.id() > 100);
        assertThat(firstTeam).extracting(HeroSearchTool.HeroSummary::id)
                .isNotEqualTo(secondTeam.stream().map(HeroSearchTool.HeroSummary::id).toList());
        assertThat(heroSearchTool.buildTeamBaseline(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), sessionId, 1, "balanced-drafter"))
                .hasSize(5)
                .extracting(HeroSearchTool.HeroSummary::id)
                .isNotEqualTo(heroSearchTool.buildTeamBaseline(
                        UUID.fromString("00000000-0000-0000-0000-000000000002"), sessionId, 1,
                        "balanced-drafter").stream().map(HeroSearchTool.HeroSummary::id).toList());
        verify(arenaApiClient, times(1)).getRound(1, sessionId);
    }

    @Test
    void teamBaselineRespectsCurrentRoundRestrictions() {
        UUID sessionId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        RoundSpec round = new RoundSpec("Restricted", 2, 50, Map.of("Fighter", 1), Map.of("Fighter", 1),
                List.of("magic"), Map.of(), "arena", List.of("Fighter", "Support"), List.of(),
                List.of(), List.of(), List.of());
        Hero banned = hero(1, "Banned");
        Hero fighter = new Hero(2, "Fighter", "fighter", null, "Fighter", 20, "good", "Marvel Comics",
                null, null, List.of("safe"), null);
        Hero support = new Hero(3, "Support", "support", null, "Support", 15, "good", "Marvel Comics",
                null, null, List.of("safe"), null);
        when(arenaApiClient.getRound(2, sessionId)).thenReturn(round);
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(50).page(0).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(List.of(banned, fighter, support));

        List<HeroSearchTool.HeroSummary> baseline = heroSearchTool.buildTeamBaseline(
                teamId, sessionId, 2, "balanced-drafter");

        assertThat(baseline).extracting(HeroSearchTool.HeroSummary::id).containsExactlyInAnyOrder(2, 3);
        assertThat(baseline.stream().mapToInt(HeroSearchTool.HeroSummary::cost).sum()).isEqualTo(35);
    }

    @Test
    void tightBudgetStillProducesDifferentAffordableTeams() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec round = new RoundSpec("Tight budget", 5, 100, Map.of(), Map.of(), List.of(), Map.of(),
                "arena", List.of(), List.of(), List.of(), List.of(), List.of());
        List<Hero> heroes = IntStream.rangeClosed(1, 140)
                .mapToObj(id -> new Hero(id, "Hero " + id, "hero-" + id, null, "Fighter",
                        id <= 9 ? 10 + id : id <= 43 ? 21 + id % 10 : 50,
                        "good", "Marvel Comics", null, null, List.of(), null))
                .toList();
        when(arenaApiClient.getRound(1, sessionId)).thenReturn(round);
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(100).page(0).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(heroes.subList(0, 100));
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(100).page(1).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(heroes.subList(100, 140));

        List<List<Integer>> squads = IntStream.rangeClosed(1, 4)
                .mapToObj(number -> heroSearchTool.buildTeamBaseline(
                        new UUID(0, number), sessionId, 1, "budget-drafter").stream()
                        .map(HeroSearchTool.HeroSummary::id).toList())
                .toList();

        assertThat(squads).allMatch(squad -> squad.size() == 5);
        assertThat(squads.stream().distinct().count()).isEqualTo(4);
        assertThat(squads).allMatch(squad -> squad.stream().filter(id -> id <= 9).count() <= 2);
        assertThat(squads).allMatch(squad -> squad.stream().filter(id -> id > 9).count() >= 3);
        assertThat(squads).allMatch(squad -> squad.stream().mapToInt(id -> heroes.get(id - 1).cost()).sum() <= 100);
    }

    @Test
    void largeRestrictedRoundRetainsEnoughAffordableHeroes() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec round = new RoundSpec("Fighters only", 20, 1000, Map.of(), Map.of(), List.of(), Map.of(),
                "ARENA_2", List.of("Fighter"), List.of(), List.of(), List.of(), List.of());
        List<Hero> heroes = IntStream.rangeClosed(1, 140)
                .mapToObj(id -> new Hero(id, "Fighter " + id, "fighter-" + id, null, "Fighter",
                        id <= 20 ? 10 : id <= 40 ? 51 : 70,
                        "good", "Marvel Comics", null, null, List.of(), null))
                .toList();
        when(arenaApiClient.getRound(3, sessionId)).thenReturn(round);
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(1000).page(0).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(heroes.subList(0, 100));
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(1000).page(1).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(heroes.subList(100, 140));

        List<HeroSearchTool.HeroSummary> baseline = heroSearchTool.buildTeamBaseline(
                UUID.randomUUID(), sessionId, 3, "balanced-drafter");

        assertThat(baseline).hasSize(20).allMatch(hero -> hero.role().equals("Fighter"));
        assertThat(baseline.stream().mapToInt(HeroSearchTool.HeroSummary::cost).sum()).isLessThanOrEqualTo(1000);
    }

    @Test
    void verifiedAlternativesFollowTheirDraftingStrategy() {
        UUID sessionId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        RoundSpec round = new RoundSpec("Open", 2, 200, Map.of(), Map.of(), List.of(), Map.of(),
                "ARENA_2", List.of(), List.of(), List.of(), List.of(), List.of());
        List<Hero> heroes = IntStream.rangeClosed(1, 4)
                .mapToObj(id -> new Hero(id, "Hero " + id, "hero-" + id,
                        new PowerStats(id * 10, id * 10, id * 10, id * 10, id * 10, id * 10),
                        "Fighter", id * 10, "good", "Marvel Comics", null, null, List.of(), null))
                .toList();
        when(arenaApiClient.getRound(5, sessionId)).thenReturn(round);
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(200).page(0).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(heroes);

        List<Integer> budget = heroSearchTool.buildTeamBaseline(teamId, sessionId, 5, "budget-drafter")
                .stream().map(HeroSearchTool.HeroSummary::id).toList();
        List<Integer> aggressive = heroSearchTool.buildTeamBaseline(teamId, sessionId, 5, "aggressive-drafter")
                .stream().map(HeroSearchTool.HeroSummary::id).toList();

        assertThat(budget).containsExactly(1, 2);
        assertThat(aggressive).containsExactly(4, 3);
    }

    @Test
    void aggressiveAlternativeCanUseAHighCostHeroWhenTheWholeSquadFits() {
        UUID sessionId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        RoundSpec round = new RoundSpec("Open", 3, 150, Map.of(), Map.of(), List.of(), Map.of(),
                "ARENA_2", List.of(), List.of(), List.of(), List.of(), List.of());
        List<Hero> heroes = IntStream.rangeClosed(1, 5)
                .mapToObj(id -> new Hero(id, "Hero " + id, "hero-" + id,
                        new PowerStats(id == 5 ? 100 : 10, id == 5 ? 100 : 10,
                                id == 5 ? 100 : 10, id == 5 ? 100 : 10,
                                id == 5 ? 100 : 10, id == 5 ? 100 : 10),
                        "Fighter", id == 5 ? 100 : 10, "good", "Marvel Comics", null, null, List.of(), null))
                .toList();
        when(arenaApiClient.getRound(6, sessionId)).thenReturn(round);
        when(arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                .maxCost(150).page(0).size(100).sortBy("cost").sortDirection("ASC").build()))
                .thenReturn(heroes);

        List<HeroSearchTool.HeroSummary> aggressive = heroSearchTool.buildTeamBaseline(
                teamId, sessionId, 6, "aggressive-drafter");

        assertThat(aggressive).hasSize(3).extracting(HeroSearchTool.HeroSummary::id).contains(5);
        assertThat(aggressive.stream().mapToInt(HeroSearchTool.HeroSummary::cost).sum()).isLessThanOrEqualTo(150);
    }

    private static Hero hero(int id, String name) {
        return new Hero(id, name, name.toLowerCase(), null, "Support", 10, "good", "Marvel Comics",
                new Appearance("Male", "Human", null, null, null, null), null,
                List.of("magic", "sorcery"), null);
    }
}

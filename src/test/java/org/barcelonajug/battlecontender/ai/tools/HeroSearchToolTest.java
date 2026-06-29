package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.AdvancedHeroSearchCriteria;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Appearance;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        var criteria = new AdvancedHeroSearchCriteria(
                "spider", "good", "Marvel Comics", "Fighter", "Male", "Human",
                null, 25,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                0, 20, "cost", "ASC");
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
        var criteria = new AdvancedHeroSearchCriteria(
                null, "good", "Marvel Comics", "Fighter", "Male", "Human",
                null, 50,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                0, 20, "cost", "ASC");
        when(arenaApiClient.getRound(3, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.advancedSearchHeroes(criteria)).thenReturn(List.of(hero(101, "Spider-Man")));

        var summaries = heroSearchTool.findHeroesForRound(sessionId.toString(), 3, null, null, null, "cost");

        assertThat(summaries).extracting(HeroSearchTool.HeroSummary::name).containsExactly("Spider-Man");
        verify(arenaApiClient).advancedSearchHeroes(criteria);
    }

    @Test
    void findHeroesForRound_supportsUnrestrictedOptionalAttributes() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Female fighters and supports",
                5,
                10_000,
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "ARENA_1",
                List.of("Fighter", "Support"),
                List.of("Female"),
                List.of(),
                List.of(),
                List.of());
        var criteria = new AdvancedHeroSearchCriteria(
                null, null, null, "Fighter", "Female", null,
                null, 10_000,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                0, 20, "power", "ASC");
        when(arenaApiClient.getRound(1, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.advancedSearchHeroes(criteria)).thenReturn(List.of(hero(201, "Captain Marvel")));

        var summaries = heroSearchTool.findHeroesForRound(
                sessionId.toString(), 1, null, "Fighter", null, "power");

        assertThat(summaries).extracting(HeroSearchTool.HeroSummary::name).containsExactly("Captain Marvel");
        verify(arenaApiClient).advancedSearchHeroes(criteria);
    }

    private static Hero hero(int id, String name) {
        return new Hero(id, name, name.toLowerCase(), null, "Support", 10, "good", "Marvel Comics",
                new Appearance("Male", "Human", null, null, null, null), null,
                List.of("magic", "sorcery"), null);
    }
}

package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

    private static Hero hero(int id, String name) {
        return new Hero(id, name, name.toLowerCase(), null, "Support", 10, "good", "Marvel Comics", null, null,
                List.of("magic", "sorcery"), null);
    }
}

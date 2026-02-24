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
    void searchHeroes_callsApiClient() {
        Hero hero = new Hero(1, "Superman", "superman", null, "Tank", 15, "good", "DC Comics", null, null, null, null);
        when(arenaApiClient.searchHeroes("Superman")).thenReturn(List.of(hero));

        List<Hero> results = heroSearchTool.searchHeroes("Superman");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Superman");
        verify(arenaApiClient).searchHeroes("Superman");
    }

    @Test
    void getHeroDetails_callsApiClient() {
        Hero hero = new Hero(1, "Superman", "superman", null, "Tank", 15, "good", "DC Comics", null, null, null, null);
        when(arenaApiClient.getHero(1)).thenReturn(hero);

        Hero result = heroSearchTool.getHeroDetails(1);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Superman");
        verify(arenaApiClient).getHero(1);
    }
}

package com.workshop.battlecontender.ai.tools;

import com.workshop.battlecontender.client.ArenaApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class HeroSearchToolTest {

    @Mock
    private ArenaApiClient arenaApiClient;

    @InjectMocks
    private HeroSearchTool heroSearchTool;

    @Test
    void searchHeroes_throwsUnsupportedOperationException_initially() {
        // Workshop participants should replace the throw statement with the actual API
        // call
        assertThatThrownBy(() -> heroSearchTool.searchHeroes("Superman"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("TODO: Implement this tool");
    }

    @Test
    void getHeroDetails_throwsUnsupportedOperationException_initially() {
        assertThatThrownBy(() -> heroSearchTool.getHeroDetails(1))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("TODO: Implement this tool");
    }
}

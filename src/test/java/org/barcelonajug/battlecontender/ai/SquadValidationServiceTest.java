package org.barcelonajug.battlecontender.ai;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SquadValidationServiceTest {

    @Mock
    private ArenaApiClient arenaApiClient;

    @InjectMocks
    private SquadValidationService squadValidationService;

    @Test
    void validateAndCalculateTotalCost_returnsTotalCost_forValidSquad() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Balanced round",
                3,
                30,
                Map.of("tank", 1, "support", 1),
                Map.of("tank", 1, "support", 1),
                List.of("cursed"),
                Map.of(),
                "city");
        when(arenaApiClient.getRound(1, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.getHero(10)).thenReturn(hero(10, "Aegis", "Tank", 10, List.of("shield")));
        when(arenaApiClient.getHero(11)).thenReturn(hero(11, "Medic", "Support", 8, List.of("healing")));
        when(arenaApiClient.getHero(12)).thenReturn(hero(12, "Blade", "Damage", 9, List.of("sharp")));

        int totalCost = squadValidationService.validateAndCalculateTotalCost(sessionId, 1, List.of(10, 11, 12));

        assertThat(totalCost).isEqualTo(27);
    }

    @Test
    void validateAndCalculateTotalCost_rejectsInvalidSquad() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Strict round",
                5,
                15,
                Map.of("tank", 1),
                Map.of("tank", 1),
                List.of("cursed"),
                Map.of(),
                "city");
        when(arenaApiClient.getRound(2, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.getHero(1)).thenReturn(hero(1, "A", "Tank", 10, List.of("cursed")));
        when(arenaApiClient.getHero(2)).thenReturn(hero(2, "B", "Damage", 10, List.of()));

        assertThatThrownBy(() -> squadValidationService.validateAndCalculateTotalCost(sessionId, 2, List.of(1, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected exactly 5 heroes but got 2.")
                .hasMessageContaining("Budget exceeded");
    }

    @Test
    void validateAndCalculateTotalCost_rejectsHeroesOutsideAllowedConstraints() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Allowed values round",
                1,
                30,
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "city",
                List.of("Tank"),
                List.of("Female"),
                List.of("Kryptonian"),
                List.of("DC Comics"),
                List.of("good"));
        when(arenaApiClient.getRound(3, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.getHero(1)).thenReturn(new Hero(1, "Mismatch", "mismatch", null, "Support", 10,
                "bad", "Marvel", new Appearance("Male", "Human", null, null, null, null), null, List.of(), null));

        assertThatThrownBy(() -> squadValidationService.validateAndCalculateTotalCost(sessionId, 3, List.of(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mismatch has role Support")
                .hasMessageContaining("Mismatch has gender Male")
                .hasMessageContaining("Mismatch has race Human")
                .hasMessageContaining("Mismatch has publisher Marvel")
                .hasMessageContaining("Mismatch has alignment bad");
    }

    @Test
    void validateAndCalculateTotalCost_treatsNullAndEmptyAllowedConstraintsAsUnrestricted() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Unrestricted round",
                1,
                30,
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                "city",
                null,
                List.of(),
                null,
                List.of(),
                null);
        when(arenaApiClient.getRound(4, sessionId)).thenReturn(roundSpec);
        when(arenaApiClient.getHero(1)).thenReturn(hero(1, "Flexible", "Support", 10, List.of()));

        assertThat(squadValidationService.validateAndCalculateTotalCost(sessionId, 4, List.of(1))).isEqualTo(10);
    }

    private static Hero hero(int id, String name, String role, int cost, List<String> tags) {
        return new Hero(id, name, name.toLowerCase(), null, role, cost, "good", "Marvel",
                new Appearance("Female", "Human", null, null, null, null), null, tags, null);
    }
}

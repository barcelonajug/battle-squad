package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "TYPESAFE_API_KEY", matches = ".+")
class TypeSafeSquadEvaluatorIT {

    @Test
    void evaluatesAValidSquadAgainstJev() {
        UUID sessionId = UUID.randomUUID();
        ArenaApiClient arena = mock(ArenaApiClient.class);
        when(arena.getRound(1, sessionId)).thenReturn(new RoundSpec(
                "A desert battle rewards fire heroes", 1, 100, Map.of(), Map.of(), List.of(),
                Map.of("fire", 1.5), "desert"));
        Hero hero = mock(Hero.class);
        when(hero.name()).thenReturn("Flame Guardian");
        when(hero.role()).thenReturn("attacker");
        when(hero.tags()).thenReturn(List.of("fire"));
        when(hero.cost()).thenReturn(25);
        when(arena.getHero(7)).thenReturn(hero);
        TypeSafeClient client = TypeSafeClient.builder().apiKey(System.getenv("TYPESAFE_API_KEY")).build();
        ObjectProvider<TypeSafeClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(client);
        DraftOption option = new DraftOption("aggressive-drafter", "Aggressive Draft", "Offensive pressure",
                new SquadRecommendation(List.of(new SquadRecommendation.HeroRef(7, "Flame Guardian")),
                        "Attack", "Fire benefits from the desert modifier", 25),
                false, true, List.of(), null, null, "NOT_EVALUATED");

        SquadEvaluator.Evaluation evaluation = new TypeSafeSquadEvaluator(provider, arena)
                .evaluate(sessionId, 1, option);

        assertThat(evaluation.score()).isBetween(0.0, 100.0);
        assertThat(evaluation.confidence()).isBetween(0.0, 1.0);
    }
}

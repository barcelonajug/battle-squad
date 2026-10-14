package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springaicommunity.typesafe.response.ScoreAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class TypeSafeSquadEvaluatorTest {

    @Test
    void evaluatesRoundAndVerifiedHeroesWithTypedQuestions() {
        UUID sessionId = UUID.randomUUID();
        ObjectProvider<TypeSafeClient> provider = mock(ObjectProvider.class);
        TypeSafeClient client = mock(TypeSafeClient.class);
        ArenaApiClient arena = mock(ArenaApiClient.class);
        when(provider.getIfAvailable()).thenReturn(client);
        when(arena.getRound(3, sessionId)).thenReturn(new RoundSpec("Desert battle", 2, 100,
                Map.of(), Map.of(), List.of(), Map.of("fire", 1.5), "desert"));
        Hero hero = mock(Hero.class);
        when(hero.name()).thenReturn("Flame");
        when(hero.role()).thenReturn("attacker");
        when(hero.tags()).thenReturn(List.of("fire"));
        when(hero.cost()).thenReturn(20);
        when(arena.getHero(7)).thenReturn(hero);
        ScoreAnswer fit = new ScoreAnswer(1.6, Map.of(), Map.of(), 0.7);
        SystemOneResponse response = mock(SystemOneResponse.class);
        when(response.score("squad_fit")).thenReturn(fit);
        when(client.systemOne(any(SystemOneRequest.class))).thenReturn(response);
        DraftOption option = new DraftOption("aggressive-drafter", "Aggressive Draft", "Offensive pressure",
                new SquadRecommendation(List.of(new SquadRecommendation.HeroRef(7, "Flame")), "Attack", "Reason", 20),
                false, true, List.of(), null, null, "NOT_EVALUATED");

        SquadEvaluator.Evaluation evaluation = new TypeSafeSquadEvaluator(provider, arena).evaluate(sessionId, 3, option);

        assertThat(evaluation.score()).isEqualTo(80.0);
        assertThat(evaluation.confidence()).isEqualTo(0.7);
        ArgumentCaptor<SystemOneRequest> request = ArgumentCaptor.forClass(SystemOneRequest.class);
        verify(client).systemOne(request.capture());
        assertThat(request.getValue().state().toDisplayString()).contains("Desert battle", "desert", "fire=1.5", "Flame");
        assertThat(request.getValue().questions()).containsOnlyKeys("squad_fit");
    }

    @Test
    void judgesStrengthWithoutInventingMissingMapEffects() {
        UUID sessionId = UUID.randomUUID();
        ObjectProvider<TypeSafeClient> provider = mock(ObjectProvider.class);
        TypeSafeClient client = mock(TypeSafeClient.class);
        ArenaApiClient arena = mock(ArenaApiClient.class);
        when(provider.getIfAvailable()).thenReturn(client);
        when(arena.getRound(4, sessionId)).thenReturn(new RoundSpec("Tournament Round", 2, 10000000,
                Map.of(), Map.of(), List.of(), Map.of(), "ARENA_2",
                List.of("Fighter"), List.of("Female"), List.of(), List.of(), List.of()));
        Hero hero = mock(Hero.class);
        when(hero.name()).thenReturn("Heroine");
        when(hero.role()).thenReturn("Fighter");
        when(hero.cost()).thenReturn(80);
        when(arena.getHero(7)).thenReturn(hero);
        SystemOneResponse response = mock(SystemOneResponse.class);
        when(response.score("squad_fit")).thenReturn(new ScoreAnswer(1.8, Map.of(), Map.of(), 0.9));
        when(client.systemOne(any(SystemOneRequest.class))).thenReturn(response);
        DraftOption option = new DraftOption("aggressive-drafter", "Aggressive Draft", "Offensive pressure",
                new SquadRecommendation(List.of(new SquadRecommendation.HeroRef(7, "Heroine")), "Attack", "Reason", 80),
                false, true, List.of(), null, null, "NOT_EVALUATED");

        SquadEvaluator.Evaluation evaluation = new TypeSafeSquadEvaluator(provider, arena).evaluate(sessionId, 4, option);

        assertThat(evaluation.score()).isEqualTo(90.0);
        assertThat(evaluation.confidence()).isEqualTo(0.9);
        ArgumentCaptor<SystemOneRequest> request = ArgumentCaptor.forClass(SystemOneRequest.class);
        verify(client).systemOne(request.capture());
        assertThat(request.getValue().questions()).containsOnlyKeys("squad_fit");
        assertThat(request.getValue().state().toDisplayString()).contains("Allowed genders: [Female]", "Allowed roles: [Fighter]");
    }
}

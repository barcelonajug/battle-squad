package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Score;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springaicommunity.typesafe.response.ScoreAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TypeSafeSquadEvaluator implements SquadEvaluator {

    private static final Score SQUAD_FIT = Score.of(
            "How competitive is this valid squad for this round? Judge its combined power statistics, role coverage, and any explicit tag bonuses. Do not reward unused budget by itself or infer powers from hero names or map labels.",
            "Weak fighting potential or poor use of explicit round bonuses",
            "Capable squad with some strengths and weaknesses",
            "Strong squad with complementary capabilities and useful round bonuses");

    private final ObjectProvider<TypeSafeClient> clientProvider;
    private final ArenaApiClient arenaApiClient;

    public TypeSafeSquadEvaluator(ObjectProvider<TypeSafeClient> clientProvider, ArenaApiClient arenaApiClient) {
        this.clientProvider = clientProvider;
        this.arenaApiClient = arenaApiClient;
    }

    @Override
    public Evaluation evaluate(UUID sessionId, int roundNo, DraftOption option) {
        TypeSafeClient client = clientProvider.getIfAvailable();
        if (client == null) {
            throw new IllegalStateException("TypeSafe API key is not configured");
        }

        RoundSpec round = arenaApiClient.getRound(roundNo, sessionId);
        List<Hero> heroes = option.recommendation().heroes().stream()
                .map(ref -> arenaApiClient.getHero(ref.id()))
                .toList();
        String state = """
                Round description: %s
                Map: %s
                Tag modifiers: %s
                Required roles: %s
                Role caps: %s
                Allowed roles: %s
                Allowed genders: %s
                Allowed races: %s
                Allowed publishers: %s
                Allowed alignments: %s
                Team size: %d
                Budget cap: %d
                Squad cost: %d
                Heroes: %s
                """.formatted(round.description(), round.mapType(), round.tagModifiers(),
                round.requiredRoles(), round.maxSameRole(), round.allowedRoles(), round.allowedGenders(),
                round.allowedRaces(), round.allowedPublishers(), round.allowedAlignments(), round.teamSize(),
                round.budgetCap(),
                option.recommendation().totalCost(), heroes.stream()
                        .map(hero -> "%s [role=%s, gender=%s, race=%s, publisher=%s, alignment=%s, tags=%s, powerstats=%s, cost=%d]".formatted(
                                hero.name(), hero.role(),
                                hero.appearance() == null ? null : hero.appearance().gender(),
                                hero.appearance() == null ? null : hero.appearance().race(),
                                hero.publisher(), hero.alignment(), hero.tags(), hero.powerstats(), hero.cost()))
                        .toList());

        SystemOneResponse response = client.systemOne(SystemOneRequest.builder()
                .state(state)
                .question("squad_fit", SQUAD_FIT)
                .build());
        ScoreAnswer fit = response.score("squad_fit");
        return new Evaluation(50 * fit.value(), fit.confidence());
    }
}

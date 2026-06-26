package org.barcelonajug.battlecontender.client;

import org.barcelonajug.battlecontender.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.util.List;
import java.util.UUID;

@Service
public class ArenaApiClient {

    private final RestClient restClient;

    public ArenaApiClient(RestClient.Builder restClientBuilder, ArenaApiProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    public List<Hero> listHeroes(int page, int size) {
        return restClient.get()
                .uri(builder -> builder.path("/api/heroes")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public Hero getHero(int id) {
        return restClient.get()
                .uri("/api/heroes/{id}", id)
                .retrieve()
                .body(Hero.class);
    }

    public List<Hero> searchHeroes(String query) {
        return restClient.get()
                .uri(builder -> builder.path("/api/heroes/search").queryParam("q", query).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<Hero> filterHeroes(String alignment, String publisher) {
        return restClient.get()
                .uri(builder -> {
                    builder.path("/api/heroes/filter");
                    if (alignment != null && !alignment.isBlank())
                        builder.queryParam("alignment", alignment);
                    if (publisher != null && !publisher.isBlank())
                        builder.queryParam("publisher", publisher);
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<Hero> advancedSearchHeroes(AdvancedHeroSearchCriteria criteria) {
        AdvancedHeroSearchResponse response = restClient.get()
                .uri(builder -> {
                    builder.path("/api/heroes/search/advanced");
                    addParam(builder, "name", criteria.name());
                    addParam(builder, "alignment", criteria.alignment());
                    addParam(builder, "publisher", criteria.publisher());
                    addParam(builder, "role", criteria.role());
                    addParam(builder, "gender", criteria.gender());
                    addParam(builder, "race", criteria.race());
                    addParam(builder, "minCost", criteria.minCost());
                    addParam(builder, "maxCost", criteria.maxCost());
                    addParam(builder, "minPower", criteria.minPower());
                    addParam(builder, "maxPower", criteria.maxPower());
                    addParam(builder, "minStrength", criteria.minStrength());
                    addParam(builder, "maxStrength", criteria.maxStrength());
                    addParam(builder, "minSpeed", criteria.minSpeed());
                    addParam(builder, "maxSpeed", criteria.maxSpeed());
                    addParam(builder, "minIntelligence", criteria.minIntelligence());
                    addParam(builder, "maxIntelligence", criteria.maxIntelligence());
                    addParam(builder, "minDurability", criteria.minDurability());
                    addParam(builder, "maxDurability", criteria.maxDurability());
                    addParam(builder, "minCombat", criteria.minCombat());
                    addParam(builder, "maxCombat", criteria.maxCombat());
                    addParam(builder, "page", criteria.page());
                    addParam(builder, "size", criteria.size());
                    addParam(builder, "sortBy", criteria.sortBy());
                    addParam(builder, "sortDirection", criteria.sortDirection());
                    return builder.build();
                })
                .retrieve()
                .body(AdvancedHeroSearchResponse.class);

        if (response == null || response._embedded() == null || response._embedded().heroList() == null) {
            return List.of();
        }
        return response._embedded().heroList();
    }

    public Session getActiveSession() {
        return restClient.get()
                .uri("/api/sessions/active")
                .retrieve()
                .body(Session.class);
    }

    public List<Session> listSessions() {
        return restClient.get()
                .uri("/api/sessions")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public UUID registerTeam(String name, List<String> members, UUID sessionId) {
        return restClient.post()
                .uri(builder -> {
                    builder.path("/api/teams/register")
                            .queryParam("name", name)
                            .queryParam("members", String.join(",", members));
                    if (sessionId != null) {
                        builder.queryParam("sessionId", sessionId.toString());
                    }
                    return builder.build();
                })
                .retrieve()
                .body(UUID.class);
    }

    public List<Team> listTeams(UUID sessionId) {
        return restClient.get()
                .uri(builder -> {
                    builder.path("/api/teams");
                    if (sessionId != null)
                        builder.queryParam("sessionId", sessionId.toString());
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<Round> listRounds(UUID sessionId) {
        return restClient.get()
                .uri(builder -> builder.path("/api/rounds")
                        .queryParam("sessionId", sessionId.toString())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public RoundSpec getRound(int roundNo, UUID sessionId) {
        return restClient.get()
                .uri(builder -> builder.path("/api/rounds/{roundNo}")
                        .queryParam("sessionId", sessionId.toString())
                        .build(roundNo))
                .retrieve()
                .body(RoundSpec.class);
    }

    public void submitSquad(int roundNo, UUID teamId, DraftSubmission submission) {
        restClient.post()
                .uri(builder -> builder.path("/api/rounds/{roundNo}/submit")
                        .queryParam("teamId", teamId.toString())
                        .build(roundNo))
                .body(submission)
                .retrieve()
                .toBodilessEntity();
    }

    public DraftSubmission getSubmission(int roundNo, UUID teamId) {
        return restClient.get()
                .uri(builder -> builder.path("/api/rounds/{roundNo}/submission")
                        .queryParam("teamId", teamId.toString())
                        .build(roundNo))
                .retrieve()
                .body(DraftSubmission.class);
    }

    private void addParam(UriBuilder builder, String name, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue && stringValue.isBlank()) {
            return;
        }
        builder.queryParam(name, value);
    }

    private record AdvancedHeroSearchResponse(EmbeddedHeroes _embedded) {
    }

    private record EmbeddedHeroes(List<Hero> heroList) {
    }
}

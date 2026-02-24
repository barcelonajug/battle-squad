package com.workshop.battlecontender.client;

import com.workshop.battlecontender.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

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
}

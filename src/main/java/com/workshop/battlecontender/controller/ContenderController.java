package com.workshop.battlecontender.controller;

import com.workshop.battlecontender.ai.BattleAdvisorService;
import com.workshop.battlecontender.ai.SquadRecommendation;
import com.workshop.battlecontender.client.ArenaApiClient;
import com.workshop.battlecontender.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contender")
public class ContenderController {

    private final ArenaApiClient arenaApiClient;
    private final BattleAdvisorService battleAdvisorService;

    public ContenderController(ArenaApiClient arenaApiClient, BattleAdvisorService battleAdvisorService) {
        this.arenaApiClient = arenaApiClient;
        this.battleAdvisorService = battleAdvisorService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerTeam(@RequestBody RegisterTeamRequest request) {
        UUID teamId = arenaApiClient.registerTeam(request.name(), request.members(), request.sessionId());
        return ResponseEntity.ok(Map.of("teamId", teamId.toString()));
    }

    @GetMapping("/session")
    public Session getActiveSession() {
        return arenaApiClient.getActiveSession();
    }

    @GetMapping("/rounds")
    public List<Round> getRounds(@RequestParam UUID sessionId) {
        return arenaApiClient.listRounds(sessionId);
    }

    @GetMapping("/round/{roundNo}")
    public RoundSpec getRoundConstraints(@PathVariable int roundNo, @RequestParam UUID sessionId) {
        return arenaApiClient.getRound(roundNo, sessionId);
    }

    @GetMapping("/heroes")
    public List<Hero> searchHeroes(@RequestParam(required = false) String q,
            @RequestParam(required = false) String alignment,
            @RequestParam(required = false) String publisher) {
        if (q != null && !q.isBlank()) {
            return arenaApiClient.searchHeroes(q);
        }
        return arenaApiClient.filterHeroes(alignment, publisher);
    }

    @PostMapping("/optimize")
    public SquadRecommendation optimizeSquad(@RequestBody OptimizeRequest request) {
        return battleAdvisorService.buildOptimalSquad(
                request.teamId(),
                request.roundNo(),
                request.sessionId());
    }

    @PostMapping("/submit")
    public ResponseEntity<Void> submitSquad(@RequestBody SubmitRequest request) {
        DraftSubmission submission = new DraftSubmission(request.heroIds(), request.strategy());
        arenaApiClient.submitSquad(request.roundNo(), request.teamId(), submission);
        return ResponseEntity.ok().build();
    }

    // --- Request DTOs ---

    public record RegisterTeamRequest(String name, List<String> members, UUID sessionId) {
    }

    public record OptimizeRequest(UUID teamId, int roundNo, UUID sessionId) {
    }

    public record SubmitRequest(UUID teamId, int roundNo, List<Integer> heroIds, String strategy) {
    }
}

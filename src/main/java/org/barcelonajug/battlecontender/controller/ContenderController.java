package org.barcelonajug.battlecontender.controller;

import org.barcelonajug.battlecontender.ai.BattleAdvisorService;
import org.barcelonajug.battlecontender.ai.DraftOptionsResponse;
import org.barcelonajug.battlecontender.ai.SquadRecommendation;
import org.barcelonajug.battlecontender.ai.SquadValidationService;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contender")
public class ContenderController {

    private static final Logger log = LoggerFactory.getLogger(ContenderController.class);

    private final ArenaApiClient arenaApiClient;
    private final BattleAdvisorService battleAdvisorService;
    private final SquadValidationService squadValidationService;

    public ContenderController(ArenaApiClient arenaApiClient, BattleAdvisorService battleAdvisorService,
            SquadValidationService squadValidationService) {
        this.arenaApiClient = arenaApiClient;
        this.battleAdvisorService = battleAdvisorService;
        this.squadValidationService = squadValidationService;
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
            @RequestParam(required = false) String publisher,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        if (q != null && !q.isBlank()) {
            return arenaApiClient.searchHeroes(q);
        }
        if ((alignment != null && !alignment.isBlank()) || (publisher != null && !publisher.isBlank())) {
            return arenaApiClient.filterHeroes(alignment, publisher);
        }
        return arenaApiClient.listHeroes(page, size);
    }

    @PostMapping("/optimize")
    public DraftOptionsResponse optimizeSquad(@RequestBody OptimizeRequest request) {
        return battleAdvisorService.buildOptimalSquad(
                request.teamId(),
                request.roundNo(),
                request.sessionId());
    }

    @PostMapping(value = "/optimize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter optimizeSquadWithProgress(@RequestBody OptimizeRequest request) {
        SseEmitter emitter = new SseEmitter(600_000L);
        Thread.startVirtualThread(() -> {
            try {
                sendEvent(emitter, "progress", new BattleAdvisorService.OptimizationProgress(0, 0, 4));
                DraftOptionsResponse response = battleAdvisorService.buildOptimalSquad(
                        request.teamId(), request.roundNo(), request.sessionId(),
                        progress -> sendEvent(emitter, "progress", progress));
                sendEvent(emitter, "result", response);
            } catch (Exception ex) {
                log.error("Squad optimization failed", ex);
                sendEvent(emitter, "error", Map.of("detail", optimizationErrorMessage(ex)));
            } finally {
                emitter.complete();
            }
        });
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(name).data(data));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String optimizationErrorMessage(Exception ex) {
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        if (cause instanceof UnauthorizedException) {
            return "OpenAI rejected the configured API key. Check OPENAI_API_KEY and restart the app.";
        }
        if (cause instanceof RateLimitException) {
            return "OpenAI is rate limiting requests. Please try again later.";
        }
        if (cause instanceof java.util.concurrent.TimeoutException) {
            return "The AI took too long to prepare a squad. Please retry; your registered team is still saved.";
        }
        if (cause instanceof IllegalArgumentException) {
            return cause.getMessage();
        }
        return "Optimization failed. Please try again or check the server logs.";
    }

    @PostMapping("/submit")
    public ResponseEntity<Void> submitSquad(@RequestBody SubmitRequest request) {
        squadValidationService.validateAndCalculateTotalCost(
                request.sessionId(),
                request.roundNo(),
                request.heroIds());
        DraftSubmission submission = new DraftSubmission(request.heroIds(), request.strategy());
        arenaApiClient.submitSquad(request.roundNo(), request.teamId(), submission);
        return ResponseEntity.ok().build();
    }

    // --- Request DTOs ---

    public record RegisterTeamRequest(String name, List<String> members, UUID sessionId) {
    }

    public record OptimizeRequest(UUID teamId, int roundNo, UUID sessionId) {
    }

    public record SubmitRequest(UUID teamId, UUID sessionId, int roundNo, List<Integer> heroIds, String strategy) {
    }
}

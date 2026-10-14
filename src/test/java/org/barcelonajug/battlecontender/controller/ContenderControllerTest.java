package org.barcelonajug.battlecontender.controller;

import org.barcelonajug.battlecontender.ai.BattleAdvisorService;
import org.barcelonajug.battlecontender.ai.DraftOption;
import org.barcelonajug.battlecontender.ai.DraftOptionsResponse;
import org.barcelonajug.battlecontender.ai.SquadRecommendation;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.DraftSubmission;
import org.barcelonajug.battlecontender.model.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ContenderController.class)
class ContenderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArenaApiClient arenaApiClient;

    @MockitoBean
    private BattleAdvisorService battleAdvisorService;

    @MockitoBean
    private org.barcelonajug.battlecontender.ai.SquadValidationService squadValidationService;

    @Test
    void getActiveSession_returnsSession() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Session mockSession = new Session(sessionId, LocalDateTime.now(), true);

        when(arenaApiClient.getActiveSession()).thenReturn(mockSession);

        mockMvc.perform(get("/api/contender/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void submitSquad_rejectsInvalidSquad() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Expected exactly 5 heroes but got 4."))
                .when(squadValidationService)
                .validateAndCalculateTotalCost(any(), anyInt(), anyList());

        mockMvc.perform(post("/api/contender/submit")
                        .contentType("application/json")
                        .content("""
                                {
                                  "teamId": "%s",
                                  "sessionId": "%s",
                                  "roundNo": 2,
                                  "heroIds": [1, 2, 3, 4],
                                  "strategy": "Test"
                                }
                                """.formatted(teamId, sessionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Expected exactly 5 heroes but got 4."));
    }

    @Test
    void optimizeSquad_returnsEvaluationAndSelectionMethod() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        DraftOption option = new DraftOption("balanced-drafter", "Balanced", "Round fit",
                new SquadRecommendation(List.of(new SquadRecommendation.HeroRef(1, "Hero")), "Balanced", "Reason", 20),
                true, true, List.of(), 80.0, 0.9, "EVALUATED");
        when(battleAdvisorService.buildOptimalSquad(teamId, 2, sessionId))
                .thenReturn(new DraftOptionsResponse(List.of(option), "balanced-drafter", "JEV"));

        mockMvc.perform(post("/api/contender/optimize")
                        .contentType("application/json")
                        .content("""
                                {"teamId":"%s","sessionId":"%s","roundNo":2}
                                """.formatted(teamId, sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectionMethod").value("JEV"))
                .andExpect(jsonPath("$.options[0].evaluationScore").value(80.0))
                .andExpect(jsonPath("$.options[0].evaluationConfidence").value(0.9))
                .andExpect(jsonPath("$.options[0].evaluationStatus").value("EVALUATED"));
    }

    @Test
    void optimizeStream_reportsProgressAndResult() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(battleAdvisorService.buildOptimalSquad(eq(teamId), eq(2), eq(sessionId), any()))
                .thenAnswer(invocation -> {
                    Consumer<BattleAdvisorService.OptimizationProgress> listener = invocation.getArgument(3);
                    listener.accept(new BattleAdvisorService.OptimizationProgress(1, 1, 4));
                    return new DraftOptionsResponse(List.of(), "balanced-drafter", "COST_FALLBACK");
                });

        MvcResult started = mockMvc.perform(post("/api/contender/optimize/stream")
                        .contentType("application/json")
                        .content("""
                                {"teamId":"%s","sessionId":"%s","roundNo":2}
                                """.formatted(teamId, sessionId)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:progress")))
                .andExpect(content().string(containsString("\"draftsCompleted\":1")))
                .andExpect(content().string(containsString("event:result")))
                .andExpect(content().string(containsString("\"selectionMethod\":\"COST_FALLBACK\"")));
    }

    @Test
    void submitSquad_returnsSuccessAfterArenaAcceptsIt() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(post("/api/contender/submit")
                        .contentType("application/json")
                        .content(submitRequest(teamId, sessionId)))
                .andExpect(status().isOk());

        verify(arenaApiClient).submitSquad(org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(teamId), any(DraftSubmission.class));
    }

    @Test
    void submitSquad_reportsAlreadySubmittedWithoutNestedJson() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String detail = "Team already submitted for round 2";
        doThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                ("{\"detail\":\"" + detail + "\",\"title\":\"Invalid State\"}").getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8))
                .when(arenaApiClient).submitSquad(anyInt(), any(), any(DraftSubmission.class));

        mockMvc.perform(post("/api/contender/submit")
                        .contentType("application/json")
                        .content(submitRequest(teamId, sessionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_SUBMITTED"))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void optimizeSquad_doesNotExposeInternalExceptionDetails() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(battleAdvisorService.buildOptimalSquad(teamId, 2, sessionId))
                .thenThrow(new IllegalStateException("private provider detail"));

        mockMvc.perform(post("/api/contender/optimize")
                        .contentType("application/json")
                        .content("""
                                {"teamId":"%s","sessionId":"%s","roundNo":2}
                                """.formatted(teamId, sessionId)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("The request failed. Check the server logs for details."));
    }

    private String submitRequest(UUID teamId, UUID sessionId) {
        return """
                {"teamId":"%s","sessionId":"%s","roundNo":2,"heroIds":[1],"strategy":"Test"}
                """.formatted(teamId, sessionId);
    }
}

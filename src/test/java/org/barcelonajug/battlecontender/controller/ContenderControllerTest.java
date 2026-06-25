package org.barcelonajug.battlecontender.controller;

import org.barcelonajug.battlecontender.ai.BattleAdvisorService;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        doThrow(new IllegalArgumentException("Expected exactly 5 heroes but got 4."))
                .when(squadValidationService)
                .validateAndCalculateTotalCost(any(), anyInt(), anyList());

        mockMvc.perform(post("/api/contender/submit")
                        .contentType("application/json")
                        .content("""
                                {
                                  "teamId": "%s",
                                  "roundNo": 2,
                                  "heroIds": [1, 2, 3, 4],
                                  "strategy": "Test"
                                }
                                """.formatted(teamId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Expected exactly 5 heroes but got 4."));
    }
}

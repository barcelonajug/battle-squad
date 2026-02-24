package com.workshop.battlecontender.controller;

import com.workshop.battlecontender.ai.BattleAdvisorService;
import com.workshop.battlecontender.client.ArenaApiClient;
import com.workshop.battlecontender.model.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
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
}

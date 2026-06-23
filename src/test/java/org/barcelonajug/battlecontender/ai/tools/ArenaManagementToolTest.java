package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.barcelonajug.battlecontender.model.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArenaManagementToolTest {

    @Mock
    private ArenaApiClient arenaApiClient;

    @InjectMocks
    private ArenaManagementTool arenaManagementTool;

    @Test
    void getActiveSession_returnsApiSession() {
        Session session = new Session(UUID.randomUUID(), LocalDateTime.parse("2026-01-01T12:00:00"), true);
        when(arenaApiClient.getActiveSession()).thenReturn(session);

        assertThat(arenaManagementTool.getActiveSession()).isEqualTo(session);
        verify(arenaApiClient).getActiveSession();
    }

    @Test
    void getRoundConstraints_parsesSessionIdAndDelegates() {
        UUID sessionId = UUID.randomUUID();
        RoundSpec roundSpec = new RoundSpec(
                "Final round",
                5,
                50,
                Map.of("tank", 1),
                Map.of("tank", 2),
                List.of("villain-only"),
                Map.of("magic", 1.5),
                "city");
        when(arenaApiClient.getRound(3, sessionId)).thenReturn(roundSpec);

        assertThat(arenaManagementTool.getRoundConstraints(3, sessionId.toString())).isEqualTo(roundSpec);
        verify(arenaApiClient).getRound(3, sessionId);
    }
}

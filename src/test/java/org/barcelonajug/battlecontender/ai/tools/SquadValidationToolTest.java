package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.ai.SquadValidationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SquadValidationToolTest {

    @Test
    void validateSquadForRound_returnsDeterministicViolations() {
        SquadValidationService validationService = mock(SquadValidationService.class);
        UUID sessionId = UUID.randomUUID();
        var expected = new SquadValidationService.ValidationResult(
                false,
                42,
                List.of("Expected exactly 5 heroes but got 4."));
        when(validationService.validateSquad(sessionId, 2, List.of(1, 2, 3, 4))).thenReturn(expected);

        SquadValidationTool tool = new SquadValidationTool(validationService);

        var actual = tool.validateSquadForRound(sessionId.toString(), 2, List.of(1, 2, 3, 4));

        assertThat(actual).isEqualTo(expected);
    }
}

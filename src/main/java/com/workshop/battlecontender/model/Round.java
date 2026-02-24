package com.workshop.battlecontender.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Round(
        UUID roundId,
        int roundNo,
        UUID sessionId,
        long seed,
        RoundSpec specJson,
        String status,
        LocalDateTime submissionDeadline) {
}

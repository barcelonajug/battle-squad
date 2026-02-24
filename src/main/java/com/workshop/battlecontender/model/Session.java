package com.workshop.battlecontender.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Session(
        UUID sessionId,
        LocalDateTime createdAt,
        boolean active
) {}

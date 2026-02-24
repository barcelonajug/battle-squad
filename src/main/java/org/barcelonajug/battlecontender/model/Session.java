package org.barcelonajug.battlecontender.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Session(
        UUID sessionId,
        LocalDateTime createdAt,
        boolean active
) {}

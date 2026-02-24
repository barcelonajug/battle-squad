package com.workshop.battlecontender.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record Team(
        UUID teamId,
        UUID sessionId,
        String name,
        LocalDateTime createdAt,
        List<String> members
) {}

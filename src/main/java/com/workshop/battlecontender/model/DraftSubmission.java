package com.workshop.battlecontender.model;

import java.util.List;

public record DraftSubmission(
        List<Integer> heroIds,
        String strategy) {
}

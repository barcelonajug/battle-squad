package org.barcelonajug.battlecontender.model;

import java.util.List;

public record DraftSubmission(
        List<Integer> heroIds,
        String strategy) {
}

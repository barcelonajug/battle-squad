package org.barcelonajug.battlecontender.ai;

import java.util.List;

public record DraftOptionsResponse(
        List<DraftOption> options,
        String recommendedStrategyId) {
}

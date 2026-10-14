package org.barcelonajug.battlecontender.ai;

import java.util.UUID;

public interface SquadEvaluator {
    Evaluation evaluate(UUID sessionId, int roundNo, DraftOption option);

    record Evaluation(double score, double confidence) {
    }
}

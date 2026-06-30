package org.barcelonajug.battlecontender.ai;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BattleAdvisorService {

    private final DeterministicSquadSolver deterministicSquadSolver;

    public BattleAdvisorService(DeterministicSquadSolver deterministicSquadSolver) {
        this.deterministicSquadSolver = deterministicSquadSolver;
    }

    public DraftOptionsResponse buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId) {
        return deterministicSquadSolver.solve(sessionId, roundNo);
    }
}

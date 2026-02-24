package org.barcelonajug.battlecontender.ai;

import java.util.List;

public record SquadRecommendation(
                List<HeroRef> heroes,
                String strategy,
                String reasoning,
                int totalCost) {
        public record HeroRef(int id, String name) {
        }
}

package org.barcelonajug.battlecontender.ai;

import java.util.List;

public final class DraftingStrategies {

    public static final List<DraftingStrategy> ALL = List.of(
            new DraftingStrategy("balanced-drafter", "Balanced Draft",
                    "General-purpose, low-risk composition."),
            new DraftingStrategy("budget-drafter", "Budget Saver",
                    "Minimize spend while staying valid."),
            new DraftingStrategy("synergy-drafter", "Synergy Draft",
                    "Lean into tags and role interactions allowed by the round."),
            new DraftingStrategy("aggressive-drafter", "Aggressive Draft",
                    "Bias toward offensive pressure and higher-impact picks."));

    private DraftingStrategies() {
    }

    public record DraftingStrategy(String id, String label, String summary) {
    }
}

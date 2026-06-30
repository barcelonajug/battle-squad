package org.barcelonajug.battlecontender.ai;

import java.util.List;
import java.util.UUID;

public interface DraftingSubagentOrchestrator {

    List<SubagentDraft> draft(UUID teamId, UUID sessionId, int roundNo);
}

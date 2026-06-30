package org.barcelonajug.battlecontender.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.List;
import java.util.UUID;

public class SpringAiDraftingSubagentOrchestrator implements DraftingSubagentOrchestrator {

    private static final String SYSTEM_PROMPT = """
            You orchestrate specialized Battle Contender drafting subagents. You do not draft squads yourself.

            For every optimization request:
            1. Use TodoWrite to expose a concise checklist for launching and collecting the four drafts.
            2. In one assistant turn, invoke Task once for each required subagent with run_in_background=true:
               balanced-drafter, budget-drafter, synergy-drafter, and aggressive-drafter.
            3. Give every subagent the team ID, session ID, round number, and instructions to return its documented JSON.
            4. Use TaskOutput with block=true to collect every background result.
            5. Return one SubagentDraft entry for each collected result. Preserve the exact strategyId.

            Never invent, repair, or optimize a squad in the parent context. If a subagent fails or its output cannot
            be understood, return its strategyId with a null recommendation and a concise error. Do not retry.
            """;

    private final ChatClient chatClient;

    public SpringAiDraftingSubagentOrchestrator(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public List<SubagentDraft> draft(UUID teamId, UUID sessionId, int roundNo) {
        String conversationId = "%s:%s:%d:subagent-orchestrator".formatted(teamId, sessionId, roundNo);
        SubagentDraftResponse response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        Build all four strategy drafts for:
                        Team: %s
                        Session: %s
                        Round: %d

                        Launch all four specialized drafting subagents in parallel and collect their results.
                        """.formatted(teamId, sessionId, roundNo))
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(SubagentDraftResponse.class);

        return response == null || response.drafts() == null ? List.of() : response.drafts();
    }
}

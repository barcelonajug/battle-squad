package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.task.TaskOutputTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springaicommunity.agent.tools.task.subagent.SubagentReference;
import org.springaicommunity.agent.tools.task.subagent.claude.ClaudeSubagentDefinition;
import org.springaicommunity.agent.tools.task.subagent.claude.ClaudeSubagentExecutor;
import org.springaicommunity.agent.tools.task.subagent.claude.ClaudeSubagentReferences;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class DraftingSubagentConfiguration {

    private static final String TASK_DESCRIPTION = """
            Delegate one battle-squad draft to a specialized drafting subagent.

            Available drafting subagents:
            %s

            Launch all four drafting subagents concurrently when optimizing a round. Use run_in_background=true,
            then collect every result with TaskOutput. Do not use built-in or unrelated subagents.
            """;

    @Bean
    DraftingSubagentOrchestrator draftingSubagentOrchestrator(
            ChatClient.Builder chatClientBuilder,
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool,
            @Value("${battle.ai.subagent-model:gpt-5-mini}") String subagentModel) {
        TodoWriteTool todoWriteTool = TodoWriteTool.builder().build();
        ChatClient.Builder miniModelBuilder = chatClientBuilder.clone()
                .defaultOptions(ChatOptions.builder().model(subagentModel).build());

        var subagentTools = DraftingSubagentTools.callbacks(
                heroSearchTool,
                arenaManagementTool,
                todoWriteTool);
        var subagentExecutor = new ClaudeSubagentExecutor(
                Map.of("default", miniModelBuilder, "mini", miniModelBuilder),
                subagentTools);
        var taskRepository = new DefaultTaskRepository();
        var subagentReferences = loadAgentReferences();

        var taskTool = TaskTool.builder()
                .subagentReferences(subagentReferences)
                .subagentExecutors(subagentExecutor)
                .taskRepository(taskRepository)
                .taskDescriptionTemplate(TASK_DESCRIPTION)
                .build();
        var taskOutputTool = TaskOutputTool.builder()
                .taskRepository(taskRepository)
                .build();

        ChatClient parentChatClient = chatClientBuilder.clone()
                .defaultToolCallbacks(taskTool, taskOutputTool)
                .defaultTools(todoWriteTool)
                .defaultAdvisors(
                        ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                        MessageChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder().maxMessages(500).build())
                                .order(Ordered.HIGHEST_PRECEDENCE + 1000)
                                .build())
                .build();

        return new SpringAiDraftingSubagentOrchestrator(parentChatClient);
    }

    private List<SubagentReference> loadAgentReferences() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:/agents/*.md");
            if (Arrays.stream(resources).allMatch(Resource::isFile)) {
                return ClaudeSubagentReferences.fromResources(resources);
            }

            // agent-utils 0.4.2 requires files in its helper, while the resolver itself supports classpath URIs.
            return Arrays.stream(resources)
                    .map(resource -> new SubagentReference(
                            "classpath:/agents/" + resource.getFilename(),
                            ClaudeSubagentDefinition.KIND,
                            null))
                    .toList();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Failed to load drafting subagent definitions.", exception);
        }
    }
}

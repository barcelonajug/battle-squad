package com.workshop.battlecontender;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("openai") // Use the default profile for testing
class BattleContenderApplicationTest {

    // Mock ChatModel so the context loads without needing a real OpenAI API key
    // during the build
    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private ChatClient.Builder chatClientBuilder;

    @Test
    void contextLoads() {
        // This test ensures the Spring application context starts successfully
        // with the provided annotations, beans, and configuration.
    }
}

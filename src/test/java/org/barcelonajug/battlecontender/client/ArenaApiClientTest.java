package org.barcelonajug.battlecontender.client;

import org.barcelonajug.battlecontender.model.Hero;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(ArenaApiClient.class)
@EnableConfigurationProperties(ArenaApiProperties.class)
@TestPropertySource(properties = "arena.api.base-url=https://test.arena.local")
class ArenaApiClientTest {

  @Autowired
  private ArenaApiClient arenaApiClient;

  @Autowired
  private MockRestServiceServer server;

  @Test
  void getHero_returnsHero_whenApiSucceeds() {
    // Arrange
    String mockJsonResponse = """
        {
          "id": 1,
          "name": "Superman",
          "slug": "superman",
          "role": "Tank",
          "cost": 15
        }
        """;

    this.server.expect(requestTo("https://test.arena.local/api/heroes/1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(mockJsonResponse, MediaType.APPLICATION_JSON));

    // Act
    Hero hero = arenaApiClient.getHero(1);

    // Assert
    assertThat(hero).isNotNull();
    assertThat(hero.name()).isEqualTo("Superman");
    assertThat(hero.role()).isEqualTo("Tank");
    assertThat(hero.cost()).isEqualTo(15);
  }
}

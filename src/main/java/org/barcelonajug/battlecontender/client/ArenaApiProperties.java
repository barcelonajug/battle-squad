package org.barcelonajug.battlecontender.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arena.api")
public record ArenaApiProperties(String baseUrl) {
}

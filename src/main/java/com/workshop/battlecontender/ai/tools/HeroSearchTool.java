package com.workshop.battlecontender.ai.tools;

import com.workshop.battlecontender.client.ArenaApiClient;
import com.workshop.battlecontender.model.Hero;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HeroSearchTool {

    private final ArenaApiClient arenaApiClient;

    public HeroSearchTool(ArenaApiClient arenaApiClient) {
        this.arenaApiClient = arenaApiClient;
    }

    @Tool(description = "Search for superheroes by name (case-insensitive substring match).")
    public List<Hero> searchHeroes(@ToolParam(description = "Search query string") String query) {
        // TODO: Call arenaApiClient.searchHeroes(query)
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }

    @Tool(description = "Filter heroes by alignment and/or publisher.")
    public List<Hero> filterHeroes(
            @ToolParam(description = "Hero alignment (e.g., good, bad, neutral)") String alignment,
            @ToolParam(description = "Comic publisher (e.g., DC Comics, Marvel Comics)") String publisher) {
        // TODO: Call arenaApiClient.filterHeroes(alignment, publisher)
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }

    @Tool(description = "Get detailed information about a specific hero by their ID.")
    public Hero getHeroDetails(@ToolParam(description = "Unique ID of the hero") int heroId) {
        // TODO: Call arenaApiClient.getHero(heroId)
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }
}

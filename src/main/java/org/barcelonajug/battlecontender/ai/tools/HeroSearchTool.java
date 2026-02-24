package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

import java.util.stream.Collectors;

@Component
public class HeroSearchTool {

    private final ArenaApiClient arenaApiClient;

    public record HeroSummary(int id, String name, String role, int cost, String alignment, List<String> tags) {
    }

    public HeroSearchTool(ArenaApiClient arenaApiClient) {
        this.arenaApiClient = arenaApiClient;
    }

    @Tool(description = "Search for superheroes by name (case-insensitive substring match).")
    public List<HeroSummary> searchHeroes(@ToolParam(description = "Search query string") String query) {
        return arenaApiClient.searchHeroes(query).stream()
                .limit(20)
                .map(h -> new HeroSummary(h.id(), h.name(), h.role(), h.cost(), h.alignment(), h.tags()))
                .collect(Collectors.toList());
    }

    @Tool(description = "Filter heroes by alignment and/or publisher.")
    public List<HeroSummary> filterHeroes(
            @ToolParam(description = "Hero alignment (e.g., good, bad, neutral)") String alignment,
            @ToolParam(description = "Comic publisher (e.g., DC Comics, Marvel Comics)") String publisher) {
        return arenaApiClient.filterHeroes(alignment, publisher).stream()
                .limit(20)
                .map(h -> new HeroSummary(h.id(), h.name(), h.role(), h.cost(), h.alignment(), h.tags()))
                .collect(Collectors.toList());
    }

    @Tool(description = "Get detailed information about a specific hero by their ID.")
    public Hero getHeroDetails(@ToolParam(description = "Unique ID of the hero") int heroId) {
        return arenaApiClient.getHero(heroId);
    }
}

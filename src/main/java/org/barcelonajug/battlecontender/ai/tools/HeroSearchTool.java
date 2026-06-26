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

    public record HeroSummary(
            int id,
            String name,
            String role,
            int cost,
            String alignment,
            String publisher,
            String gender,
            String race,
            List<String> tags) {
    }

    public HeroSearchTool(ArenaApiClient arenaApiClient) {
        this.arenaApiClient = arenaApiClient;
    }

    @Tool(description = "Search for superheroes by name (case-insensitive substring match).")
    public List<HeroSummary> searchHeroes(@ToolParam(description = "Search query string") String query) {
        // TODO: Call arenaApiClient.searchHeroes(query).stream().limit(20).map(h -> new
        // HeroSummary(...)).collect(Collectors.toList())
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }

    @Tool(description = "Filter heroes by alignment and/or publisher.")
    public List<HeroSummary> filterHeroes(
            @ToolParam(description = "Hero alignment (e.g., good, bad, neutral)") String alignment,
            @ToolParam(description = "Comic publisher (e.g., DC Comics, Marvel Comics)") String publisher) {
        // TODO: Call arenaApiClient.filterHeroes(alignment,
        // publisher).stream().limit(20).map(h -> new
        // HeroSummary(...)).collect(Collectors.toList())
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }

    @Tool(description = "Advanced hero search with filters for name, role, gender, race, publisher, alignment, budget, power stats, sorting, and pagination.")
    public List<HeroSummary> advancedSearchHeroes(
            @ToolParam(description = "Optional hero name filter") String name,
            @ToolParam(description = "Optional alignment filter: good, bad, or neutral") String alignment,
            @ToolParam(description = "Optional comic publisher filter") String publisher,
            @ToolParam(description = "Optional role filter, for example Tank, Fighter, Support, or Assassin") String role,
            @ToolParam(description = "Optional gender filter") String gender,
            @ToolParam(description = "Optional race or species filter") String race,
            @ToolParam(description = "Optional maximum cost") Integer maxCost,
            @ToolParam(description = "Optional sort field: name, cost, power, or speed") String sortBy,
            @ToolParam(description = "Optional sort direction: ASC or DESC") String sortDirection) {
        // TODO: Build an AdvancedHeroSearchCriteria and call
        // arenaApiClient.advancedSearchHeroes(criteria). Map the returned heroes to
        // HeroSummary records and cap the response to a practical size for the model.
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }

    @Tool(description = "Find heroes that fit the active round constraints. Use this before drafting a squad for a round.")
    public List<HeroSummary> findHeroesForRound(
            @ToolParam(description = "Session UUID as a string") String sessionId,
            @ToolParam(description = "Round number") int roundNo,
            @ToolParam(description = "Optional hero name filter") String name,
            @ToolParam(description = "Optional preferred role filter") String role,
            @ToolParam(description = "Optional maximum cost. Defaults to the round budget cap when omitted.") Integer maxCost,
            @ToolParam(description = "Optional sort field: name, cost, power, or speed") String sortBy) {
        // TODO: Load the RoundSpec with arenaApiClient.getRound(roundNo, UUID.fromString(sessionId)).
        // TODO: Apply allowedRoles, allowedGenders, allowedRaces, allowedPublishers,
        // allowedAlignments, and budgetCap to AdvancedHeroSearchCriteria.
        // TODO: Query /api/heroes/search/advanced through arenaApiClient, merge unique
        // heroes when multiple allowed values require multiple calls, then return
        // HeroSummary records.
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }

    @Tool(description = "Get detailed information about a specific hero by their ID.")
    public Hero getHeroDetails(@ToolParam(description = "Unique ID of the hero") int heroId) {
        // TODO: Call arenaApiClient.getHero(heroId)
        throw new UnsupportedOperationException("TODO: Implement this tool");
    }
}

package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.AdvancedHeroSearchCriteria;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        return arenaApiClient.searchHeroes(query).stream()
                .limit(20)
                .map(this::toSummary)
                .toList();
    }

    @Tool(description = "Filter heroes by alignment and/or publisher.")
    public List<HeroSummary> filterHeroes(
            @ToolParam(description = "Hero alignment (e.g., good, bad, neutral)") String alignment,
            @ToolParam(description = "Comic publisher (e.g., DC Comics, Marvel Comics)") String publisher) {
        return arenaApiClient.filterHeroes(alignment, publisher).stream()
                .limit(20)
                .map(this::toSummary)
                .toList();
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
        return arenaApiClient.advancedSearchHeroes(new AdvancedHeroSearchCriteria(
                name, alignment, publisher, role, gender, race,
                null, maxCost,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                0, 20, sortBy, sortDirection)).stream()
                .limit(20)
                .map(this::toSummary)
                .toList();
    }

    @Tool(description = "Find heroes that fit the active round constraints. Use this before drafting a squad for a round.")
    public List<HeroSummary> findHeroesForRound(
            @ToolParam(description = "Session UUID as a string") String sessionId,
            @ToolParam(description = "Round number") int roundNo,
            @ToolParam(description = "Optional hero name filter") String name,
            @ToolParam(description = "Optional preferred role filter") String role,
            @ToolParam(description = "Optional maximum cost. Defaults to the round budget cap when omitted.") Integer maxCost,
            @ToolParam(description = "Optional sort field: name, cost, power, or speed") String sortBy) {
        RoundSpec roundSpec = arenaApiClient.getRound(roundNo, UUID.fromString(sessionId));
        Integer effectiveMaxCost = maxCost == null ? roundSpec.budgetCap() : Math.min(maxCost, roundSpec.budgetCap());

        List<String> roles = constrainedValues(roundSpec.allowedRoles(), role);
        List<String> alignments = constrainedValues(roundSpec.allowedAlignments(), null);
        List<String> publishers = constrainedValues(roundSpec.allowedPublishers(), null);
        List<String> genders = constrainedValues(roundSpec.allowedGenders(), null);
        List<String> races = constrainedValues(roundSpec.allowedRaces(), null);

        Map<Integer, Hero> uniqueHeroes = new LinkedHashMap<>();
        for (String roleValue : roles) {
            for (String alignmentValue : alignments) {
                for (String publisherValue : publishers) {
                    for (String genderValue : genders) {
                        for (String raceValue : races) {
                            List<Hero> heroes = arenaApiClient.advancedSearchHeroes(new AdvancedHeroSearchCriteria(
                                    name, alignmentValue, publisherValue, roleValue, genderValue, raceValue,
                                    null, effectiveMaxCost,
                                    null, null,
                                    null, null,
                                    null, null,
                                    null, null,
                                    null, null,
                                    null, null,
                                    0, 20, sortBy, "ASC"));
                            for (Hero hero : heroes) {
                                uniqueHeroes.putIfAbsent(hero.id(), hero);
                                if (uniqueHeroes.size() >= 40) {
                                    return uniqueHeroes.values().stream().map(this::toSummary).toList();
                                }
                            }
                        }
                    }
                }
            }
        }

        return uniqueHeroes.values().stream()
                .map(this::toSummary)
                .toList();
    }

    @Tool(description = "Get detailed information about a specific hero by their ID.")
    public Hero getHeroDetails(@ToolParam(description = "Unique ID of the hero") int heroId) {
        return arenaApiClient.getHero(heroId);
    }

    private HeroSummary toSummary(Hero hero) {
        List<String> tags = hero.tags() == null ? List.of() : hero.tags().stream()
                .map(String::valueOf)
                .toList();

        return new HeroSummary(
                hero.id(),
                hero.name(),
                hero.role(),
                hero.cost(),
                hero.alignment(),
                hero.publisher(),
                hero.appearance() == null ? null : hero.appearance().gender(),
                hero.appearance() == null ? null : hero.appearance().race(),
                tags);
    }

    private List<String> constrainedValues(List<String> allowedValues, String requestedValue) {
        if (requestedValue != null && !requestedValue.isBlank()) {
            return List.of(requestedValue);
        }
        if (allowedValues == null || allowedValues.isEmpty()) {
            return List.of((String) null);
        }
        return new ArrayList<>(allowedValues);
    }
}

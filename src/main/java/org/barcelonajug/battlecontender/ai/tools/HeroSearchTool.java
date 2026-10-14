package org.barcelonajug.battlecontender.ai.tools;

import org.barcelonajug.battlecontender.client.AdvancedHeroSearchCriteria;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.PowerStats;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Component
public class HeroSearchTool {

    private static final int POOL_PAGE_SIZE = 100;
    private static final int MAX_POOL_PAGES = 20;
    private static final int TEAM_CANDIDATE_COUNT = 60;

    private final ArenaApiClient arenaApiClient;
    private final Map<RoundKey, RoundCandidatePool> roundPools = new ConcurrentHashMap<>();

    private record RoundKey(UUID sessionId, int roundNo) { }

    private record RoundCandidatePool(RoundSpec round, List<Hero> heroes) { }

    private record RoundSearchCriteria(
            String name,
            String alignment,
            String publisher,
            String role,
            String gender,
            String race,
            Integer maxCost,
            String sortBy) {
    }

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

    public List<HeroSummary> findHeroesForTeamRound(UUID teamId, UUID sessionId, int roundNo,
            String strategyId) {
        RoundCandidatePool pool = roundPools.computeIfAbsent(new RoundKey(sessionId, roundNo), this::loadRoundPool);
        RoundSpec round = pool.round();
        Comparator<Hero> teamOrder = Comparator
                .comparingLong((Hero hero) -> cohortRank(teamId, strategyId, hero.id()))
                .thenComparingInt(Hero::id);
        Map<Integer, Hero> selected = new LinkedHashMap<>();

        if (round.requiredRoles() != null) {
            round.requiredRoles().forEach((role, count) -> pool.heroes().stream()
                    .filter(hero -> normalized(hero.role()).equals(normalized(role)))
                    .sorted(teamOrder)
                    .limit(Math.max(round.teamSize(), count * 4L))
                    .forEach(hero -> selected.putIfAbsent(hero.id(), hero)));
        }

        int affordableCost = round.budgetCap() / Math.max(1, round.teamSize());
        long affordableCount = pool.heroes().stream().filter(hero -> hero.cost() <= affordableCost).count();
        int affordableSample = affordableCount > 100 ? 25 : Math.max(1, round.teamSize());
        int middleCost = Math.min(round.budgetCap(), affordableCost + Math.max(10, affordableCost / 2));
        List<Hero> affordable = pool.heroes().stream().filter(hero -> hero.cost() <= affordableCost)
                .sorted(Comparator.comparingInt(Hero::cost)).toList();
        List<Hero> middle = pool.heroes().stream()
                .filter(hero -> hero.cost() > affordableCost && hero.cost() <= middleCost)
                .sorted(Comparator.comparingInt(Hero::cost)).toList();
        boolean openRound = (round.requiredRoles() == null || round.requiredRoles().isEmpty())
                && (round.maxSameRole() == null || round.maxSameRole().isEmpty());
        boolean variedAffordableMix = openRound && round.teamSize() > 2 && round.teamSize() <= 10
                && affordable.size() >= 2 && middle.size() >= round.teamSize() - 2
                && affordable.stream().limit(2).mapToInt(Hero::cost).sum()
                        + middle.stream().limit(round.teamSize() - 2).mapToInt(Hero::cost).sum()
                        <= round.budgetCap();
        if (variedAffordableMix) affordableSample = 2;
        if (round.teamSize() > 10) {
            affordable.stream().limit(round.teamSize())
                    .forEach(hero -> selected.putIfAbsent(hero.id(), hero));
        }
        pool.heroes().stream().filter(hero -> hero.cost() <= affordableCost)
                .sorted(teamOrder).limit(affordableSample)
                .forEach(hero -> selected.putIfAbsent(hero.id(), hero));
        pool.heroes().stream().filter(hero -> hero.cost() > affordableCost && hero.cost() <= middleCost)
                .sorted(teamOrder).limit(25)
                .forEach(hero -> selected.putIfAbsent(hero.id(), hero));
        pool.heroes().stream().filter(hero -> hero.cost() > middleCost)
                .sorted(teamOrder).limit(10)
                .forEach(hero -> selected.putIfAbsent(hero.id(), hero));
        pool.heroes().stream().filter(hero -> !variedAffordableMix || hero.cost() > affordableCost)
                .sorted(teamOrder).limit(TEAM_CANDIDATE_COUNT)
                .forEach(hero -> selected.putIfAbsent(hero.id(), hero));

        return selected.values().stream().limit(TEAM_CANDIDATE_COUNT).map(this::toSummary).toList();
    }

    public List<HeroSummary> buildTeamBaseline(UUID teamId, UUID sessionId, int roundNo,
            String strategyId) {
        List<HeroSummary> candidates = findHeroesForTeamRound(teamId, sessionId, roundNo, strategyId);
        RoundSpec round = roundPools.get(new RoundKey(sessionId, roundNo)).round();
        Comparator<HeroSummary> byCost = Comparator.comparingInt(HeroSummary::cost)
                .thenComparingLong(hero -> cohortRank(teamId, strategyId, hero.id()));
        List<HeroSummary> ordered = candidates.stream().sorted(byCost).toList();
        Map<Integer, Hero> heroById = new HashMap<>();
        roundPools.get(new RoundKey(sessionId, roundNo)).heroes()
                .forEach(hero -> heroById.put(hero.id(), hero));
        Comparator<HeroSummary> teamRank = Comparator.comparingLong(
                hero -> cohortRank(teamId, strategyId, hero.id()));
        Comparator<HeroSummary> strategyOrder = switch (strategyId == null ? "" : strategyId) {
            case "budget-drafter" -> byCost;
            case "aggressive-drafter" -> Comparator
                    .comparingInt((HeroSummary hero) -> offensiveStats(heroById.get(hero.id())))
                    .reversed().thenComparing(teamRank);
            case "balanced-drafter" -> Comparator
                    .comparingInt((HeroSummary hero) -> totalStats(heroById.get(hero.id())))
                    .reversed().thenComparing(teamRank);
            default -> teamRank;
        };
        List<HeroSummary> teamOrder = candidates.stream().sorted(strategyOrder).toList();
        List<HeroSummary> picks = new ArrayList<>();
        Map<String, Integer> roleCounts = new HashMap<>();

        if (round.requiredRoles() != null) {
            for (Map.Entry<String, Integer> requirement : round.requiredRoles().entrySet()) {
                for (int i = 0; i < requirement.getValue(); i++) {
                    HeroSummary pick = ordered.stream()
                            .filter(hero -> normalized(hero.role()).equals(normalized(requirement.getKey())))
                            .filter(hero -> !picks.contains(hero))
                            .filter(hero -> canAddRole(hero, round, roleCounts))
                            .findFirst().orElse(null);
                    if (pick == null) return List.of();
                    picks.add(pick);
                    roleCounts.merge(normalized(pick.role()), 1, Integer::sum);
                }
            }
        }

        int preferredCost = "aggressive-drafter".equals(strategyId) ? round.budgetCap() : Math.min(round.budgetCap(),
                round.budgetCap() / Math.max(1, round.teamSize()) * 3 / 2);
        for (HeroSummary candidate : teamOrder) {
            if (picks.size() >= round.teamSize()) break;
            if (candidate.cost() > preferredCost || picks.contains(candidate)
                    || !canAddRole(candidate, round, roleCounts)
                    || !canCompleteWithinBudget(candidate, picks, ordered, round)) continue;
            picks.add(candidate);
            roleCounts.merge(normalized(candidate.role()), 1, Integer::sum);
        }
        for (HeroSummary candidate : ordered) {
            if (picks.size() >= round.teamSize()) break;
            if (picks.contains(candidate) || !canAddRole(candidate, round, roleCounts)
                    || !canCompleteWithinBudget(candidate, picks, ordered, round)) continue;
            picks.add(candidate);
            roleCounts.merge(normalized(candidate.role()), 1, Integer::sum);
        }
        if (picks.size() != round.teamSize()
                || picks.stream().mapToInt(HeroSummary::cost).sum() > round.budgetCap()) {
            return List.of();
        }
        return List.copyOf(picks);
    }

    private int offensiveStats(Hero hero) {
        PowerStats stats = hero == null ? null : hero.powerstats();
        return stats == null ? 0 : stats.strength() + stats.power() + stats.speed() + stats.combat();
    }

    private int totalStats(Hero hero) {
        PowerStats stats = hero == null ? null : hero.powerstats();
        return stats == null ? 0 : stats.durability() + stats.strength() + stats.power()
                + stats.speed() + stats.intelligence() + stats.combat();
    }

    private boolean canCompleteWithinBudget(HeroSummary candidate, List<HeroSummary> picks,
            List<HeroSummary> ordered, RoundSpec round) {
        int remaining = round.teamSize() - picks.size() - 1;
        int minimumRemainder = ordered.stream()
                .filter(hero -> hero.id() != candidate.id() && !picks.contains(hero))
                .limit(remaining)
                .mapToInt(HeroSummary::cost).sum();
        int costSoFar = picks.stream().mapToInt(HeroSummary::cost).sum();
        return costSoFar + candidate.cost() + minimumRemainder <= round.budgetCap();
    }

    private boolean canAddRole(HeroSummary hero, RoundSpec round, Map<String, Integer> roleCounts) {
        if (round.maxSameRole() == null) return true;
        String role = normalized(hero.role());
        return round.maxSameRole().entrySet().stream()
                .filter(entry -> normalized(entry.getKey()).equals(role))
                .allMatch(entry -> roleCounts.getOrDefault(role, 0) < entry.getValue());
    }

    private long cohortRank(UUID teamId, String strategyId, int heroId) {
        long value = teamId.getMostSignificantBits()
                ^ Long.rotateLeft(teamId.getLeastSignificantBits(), 17)
                ^ ((long) strategyId.hashCode() << 32)
                ^ (heroId * 0x9e3779b97f4a7c15L);
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private RoundCandidatePool loadRoundPool(RoundKey key) {
        RoundSpec round = arenaApiClient.getRound(key.roundNo(), key.sessionId());
        Map<Integer, Hero> heroes = new LinkedHashMap<>();
        for (int page = 0; page < MAX_POOL_PAGES; page++) {
            List<Hero> results = arenaApiClient.advancedSearchHeroes(AdvancedHeroSearchCriteria.builder()
                    .maxCost(round.budgetCap()).page(page).size(POOL_PAGE_SIZE)
                    .sortBy("cost").sortDirection("ASC").build());
            if (results == null || results.isEmpty()) break;
            int previousSize = heroes.size();
            results.forEach(hero -> heroes.putIfAbsent(hero.id(), hero));
            if (results.size() < POOL_PAGE_SIZE || heroes.size() == previousSize) break;
        }
        return new RoundCandidatePool(round,
                heroes.values().stream().filter(hero -> eligibleForRound(hero, round)).toList());
    }

    private boolean eligibleForRound(Hero hero, RoundSpec round) {
        if (hero.cost() > round.budgetCap()
                || !allowed(round.allowedRoles(), hero.role())
                || !allowed(round.allowedAlignments(), hero.alignment())
                || !allowed(round.allowedPublishers(), hero.publisher())
                || !allowed(round.allowedGenders(), hero.appearance() == null ? null : hero.appearance().gender())
                || !allowed(round.allowedRaces(), hero.appearance() == null ? null : hero.appearance().race())) {
            return false;
        }
        if (round.maxSameRole() != null && round.maxSameRole().entrySet().stream()
                .anyMatch(entry -> normalized(entry.getKey()).equals(normalized(hero.role())) && entry.getValue() < 1)) {
            return false;
        }
        return round.bannedTags() == null || hero.tags() == null || hero.tags().stream()
                .noneMatch(tag -> round.bannedTags().stream()
                        .anyMatch(banned -> normalized(banned).equals(normalized(tag))));
    }

    private boolean allowed(List<String> values, String value) {
        return values == null || values.isEmpty() || values.stream()
                .anyMatch(allowed -> normalized(allowed).equals(normalized(value)));
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
        return searchAndSummarize(buildCriteria(name, alignment, publisher, role, gender, race, maxCost, sortBy, sortDirection));
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

        return searchRoundHeroes(name, effectiveMaxCost, sortBy, roles, alignments, publishers, genders, races);
    }

    @Tool(description = "Get detailed information about a specific hero by their ID.")
    public Hero getHeroDetails(@ToolParam(description = "Unique ID of the hero") int heroId) {
        return arenaApiClient.getHero(heroId);
    }

    private HeroSummary toSummary(Hero hero) {
        List<String> tags = hero.tags() == null ? List.of() : hero.tags().stream().map(String::valueOf).toList();

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

    private AdvancedHeroSearchCriteria buildCriteria(
            String name,
            String alignment,
            String publisher,
            String role,
            String gender,
            String race,
            Integer maxCost,
            String sortBy,
            String sortDirection) {
        return AdvancedHeroSearchCriteria.builder()
                .name(name)
                .alignment(alignment)
                .publisher(publisher)
                .role(role)
                .gender(gender)
                .race(race)
                .maxCost(maxCost)
                .page(0)
                .size(20)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }

    private List<HeroSummary> searchAndSummarize(AdvancedHeroSearchCriteria criteria) {
        return arenaApiClient.advancedSearchHeroes(criteria).stream()
                .limit(20)
                .map(this::toSummary)
                .toList();
    }

    private Stream<RoundSearchCriteria> combinations(
            String name,
            Integer maxCost,
            String sortBy,
            List<String> roles,
            List<String> alignments,
            List<String> publishers,
            List<String> genders,
            List<String> races) {
        return roles.stream()
                .flatMap(role -> alignments.stream()
                        .flatMap(alignment -> publishers.stream()
                                .flatMap(publisher -> genders.stream()
                                        .flatMap(gender -> races.stream()
                                                .map(race -> new RoundSearchCriteria(name, alignment, publisher, role, gender, race, maxCost, sortBy))))));
    }

    private List<Hero> searchHeroes(
            RoundSearchCriteria criteria) {
        return arenaApiClient.advancedSearchHeroes(buildCriteria(
                criteria.name(),
                criteria.alignment(),
                criteria.publisher(),
                criteria.role(),
                criteria.gender(),
                criteria.race(),
                criteria.maxCost(),
                criteria.sortBy(),
                "ASC"));
    }

    private List<HeroSummary> searchRoundHeroes(
            String name,
            Integer maxCost,
            String sortBy,
            List<String> roles,
            List<String> alignments,
            List<String> publishers,
            List<String> genders,
            List<String> races) {
        return combinations(name, maxCost, sortBy, roles, alignments, publishers, genders, races)
                .flatMap(criteria -> searchHeroes(criteria).stream())
                .filter(distinctByKey(Hero::id))
                .limit(40)
                .map(this::toSummary)
                .toList();
    }

    private List<String> constrainedValues(List<String> allowedValues, String requestedValue) {
        if (requestedValue != null && !requestedValue.isBlank()) {
            return List.of(requestedValue);
        }
        if (allowedValues == null || allowedValues.isEmpty()) {
            return Collections.singletonList(null);
        }
        return new ArrayList<>(allowedValues);
    }

    private static <T> java.util.function.Predicate<T> distinctByKey(java.util.function.Function<? super T, ?> keyExtractor) {
        Set<Object> seen = new HashSet<>();
        return value -> seen.add(keyExtractor.apply(value));
    }
}

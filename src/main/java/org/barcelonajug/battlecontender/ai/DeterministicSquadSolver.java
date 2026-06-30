package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.PowerStats;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeterministicSquadSolver {

    private static final int HERO_PAGE_SIZE = 200;
    private static final int MAX_HERO_PAGES = 100;
    private static final int BEAM_WIDTH = 4_000;

    private static final List<Strategy> STRATEGIES = List.of(
            new Strategy("balanced-drafter", "Balanced Draft", "General-purpose, low-risk composition.",
                    Scoring.BALANCED),
            new Strategy("budget-drafter", "Budget Saver", "Minimize spend while staying valid.",
                    Scoring.BUDGET),
            new Strategy("synergy-drafter", "Synergy Draft",
                    "Lean into tags and role interactions allowed by the round.", Scoring.SYNERGY),
            new Strategy("aggressive-drafter", "Aggressive Draft",
                    "Bias toward offensive pressure and higher-impact picks.", Scoring.AGGRESSIVE));

    private final ArenaApiClient arenaApiClient;
    private final SquadValidationService squadValidationService;

    public DeterministicSquadSolver(
            ArenaApiClient arenaApiClient,
            SquadValidationService squadValidationService) {
        this.arenaApiClient = arenaApiClient;
        this.squadValidationService = squadValidationService;
    }

    public DraftOptionsResponse solve(UUID sessionId, int roundNo) {
        RoundSpec roundSpec = arenaApiClient.getRound(roundNo, sessionId);
        List<Hero> eligibleHeroes = loadAllHeroes().stream()
                .filter(hero -> isIndividuallyEligible(hero, roundSpec))
                .toList();

        Set<List<Integer>> usedSquads = new HashSet<>();
        List<DraftOption> options = new ArrayList<>();
        for (Strategy strategy : STRATEGIES) {
            DraftOption option = buildOption(sessionId, roundNo, roundSpec, eligibleHeroes, strategy, usedSquads);
            options.add(option);
            if (option.valid()) {
                usedSquads.add(canonicalIds(option.recommendation().heroes()));
            }
        }

        Optional<DraftOption> recommended = options.stream()
                .filter(DraftOption::valid)
                .min(Comparator.comparingInt(option -> option.recommendation().totalCost()));
        List<DraftOption> finalizedOptions = options.stream()
                .map(option -> recommended
                        .filter(value -> value.strategyId().equals(option.strategyId()))
                        .map(value -> new DraftOption(
                                option.strategyId(),
                                option.label(),
                                option.summary(),
                                option.recommendation(),
                                true,
                                true,
                                List.of()))
                        .orElse(option))
                .toList();

        return new DraftOptionsResponse(
                finalizedOptions,
                recommended.map(DraftOption::strategyId).orElse(null));
    }

    private DraftOption buildOption(UUID sessionId, int roundNo, RoundSpec roundSpec, List<Hero> eligibleHeroes,
            Strategy strategy, Set<List<Integer>> usedSquads) {
        List<Hero> candidates = eligibleHeroes.stream()
                .sorted(heroComparator(strategy, roundSpec))
                .toList();
        List<SquadState> rankedSquads = search(candidates, roundSpec, strategy);

        for (SquadState state : rankedSquads) {
            List<Integer> ids = state.heroes().stream().map(Hero::id).sorted().toList();
            if (usedSquads.contains(ids)) {
                continue;
            }

            SquadValidationService.ValidationResult validation = squadValidationService.validateSquad(
                    sessionId, roundNo, ids);
            if (!validation.valid()) {
                continue;
            }

            SquadRecommendation recommendation = new SquadRecommendation(
                    state.heroes().stream()
                            .sorted(Comparator.comparingInt(Hero::id))
                            .map(hero -> new SquadRecommendation.HeroRef(hero.id(), hero.name()))
                            .toList(),
                    strategy.label(),
                    reasoning(strategy, roundSpec, state, validation.totalCost()),
                    validation.totalCost());
            return new DraftOption(
                    strategy.id(),
                    strategy.label(),
                    strategy.summary(),
                    recommendation,
                    false,
                    true,
                    List.of());
        }

        String violation = "No distinct valid squad was found for this strategy.";
        return new DraftOption(
                strategy.id(),
                strategy.label(),
                strategy.summary(),
                new SquadRecommendation(List.of(), strategy.label(), violation, 0),
                false,
                false,
                List.of(violation));
    }

    private List<SquadState> search(List<Hero> candidates, RoundSpec roundSpec, Strategy strategy) {
        List<SquadState> beam = List.of(new SquadState(List.of(), 0, 0));
        Map<String, int[]> suffixRoleCounts = suffixRoleCounts(candidates, roundSpec);
        Comparator<SquadState> comparator = stateComparator(strategy, roundSpec);
        for (int slot = 0; slot < roundSpec.teamSize(); slot++) {
            PriorityQueue<SquadState> expanded = new PriorityQueue<>(BEAM_WIDTH + 1, comparator.reversed());
            for (SquadState state : beam) {
                for (int index = state.nextIndex(); index < candidates.size(); index++) {
                    Hero hero = candidates.get(index);
                    int nextCost = state.cost() + hero.cost();
                    if (nextCost > roundSpec.budgetCap()) {
                        continue;
                    }

                    List<Hero> heroes = new ArrayList<>(state.heroes());
                    heroes.add(hero);
                    if (exceedsRoleCap(heroes, roundSpec)) {
                        continue;
                    }
                    int nextIndex = index + 1;
                    int remainingSlots = roundSpec.teamSize() - heroes.size();
                    if (!canStillMeetRequiredRoles(
                            heroes, nextIndex, remainingSlots, roundSpec, suffixRoleCounts)) {
                        continue;
                    }
                    retainBest(expanded, new SquadState(List.copyOf(heroes), nextIndex, nextCost), comparator);
                }
            }

            beam = expanded.stream()
                    .sorted(comparator)
                    .toList();
            if (beam.isEmpty()) {
                return List.of();
            }
        }

        return beam.stream()
                .filter(state -> meetsRequiredRoles(state.heroes(), roundSpec))
                .sorted(stateComparator(strategy, roundSpec))
                .toList();
    }

    private void retainBest(PriorityQueue<SquadState> states, SquadState candidate,
            Comparator<SquadState> bestFirstComparator) {
        if (states.size() < BEAM_WIDTH) {
            states.add(candidate);
            return;
        }
        SquadState worst = states.peek();
        if (bestFirstComparator.compare(candidate, worst) < 0) {
            states.poll();
            states.add(candidate);
        }
    }

    private Map<String, int[]> suffixRoleCounts(List<Hero> candidates, RoundSpec roundSpec) {
        Map<String, int[]> counts = new HashMap<>();
        requiredRoles(roundSpec).keySet().forEach(role -> counts.put(role, new int[candidates.size() + 1]));
        for (int index = candidates.size() - 1; index >= 0; index--) {
            String candidateRole = normalize(candidates.get(index).role());
            for (Map.Entry<String, int[]> entry : counts.entrySet()) {
                entry.getValue()[index] = entry.getValue()[index + 1]
                        + (entry.getKey().equals(candidateRole) ? 1 : 0);
            }
        }
        return counts;
    }

    private boolean canStillMeetRequiredRoles(List<Hero> heroes, int nextIndex, int remainingSlots,
            RoundSpec roundSpec, Map<String, int[]> suffixRoleCounts) {
        int totalDeficit = 0;
        for (Map.Entry<String, Integer> requirement : requiredRoles(roundSpec).entrySet()) {
            int deficit = Math.max(0, requirement.getValue() - (int) countRole(heroes, requirement.getKey()));
            totalDeficit += deficit;
            if (deficit > suffixRoleCounts.get(requirement.getKey())[nextIndex]) {
                return false;
            }
        }
        return totalDeficit <= remainingSlots;
    }

    private Comparator<Hero> heroComparator(Strategy strategy, RoundSpec roundSpec) {
        return Comparator
                .comparingDouble((Hero hero) -> individualScore(hero, strategy.scoring(), roundSpec))
                .reversed()
                .thenComparingInt(Hero::cost)
                .thenComparingInt(Hero::id);
    }

    private Comparator<SquadState> stateComparator(Strategy strategy, RoundSpec roundSpec) {
        return Comparator
                .comparingDouble((SquadState state) -> partialScore(state, strategy.scoring(), roundSpec))
                .reversed()
                .thenComparingInt(SquadState::cost)
                .thenComparing(state -> state.heroes().stream().map(Hero::id).sorted().toList(),
                        this::compareIds);
    }

    private double partialScore(SquadState state, Scoring scoring, RoundSpec roundSpec) {
        long satisfiedRequiredSlots = requiredRoles(roundSpec).entrySet().stream()
                .mapToLong(entry -> Math.min(entry.getValue(), countRole(state.heroes(), entry.getKey())))
                .sum();
        return satisfiedRequiredSlots * 1_000_000d + squadScore(state.heroes(), scoring, roundSpec);
    }

    private double squadScore(List<Hero> heroes, Scoring scoring, RoundSpec roundSpec) {
        int totalStats = heroes.stream().mapToInt(this::totalStats).sum();
        int totalCost = heroes.stream().mapToInt(Hero::cost).sum();
        return switch (scoring) {
            case BALANCED -> totalStats
                    - aggregateStatSpread(heroes) * 2d
                    + distinctRoles(heroes) * 100d
                    + tagModifierScore(heroes, roundSpec) * 10d;
            case BUDGET -> -totalCost * 1_000_000d + totalStats;
            case SYNERGY -> tagModifierScore(heroes, roundSpec) * 1_000d
                    + repeatedTagPairs(heroes) * 100d
                    + totalStats;
            case AGGRESSIVE -> heroes.stream().mapToInt(this::aggressionStats).sum() * 100d - totalCost;
        };
    }

    private double individualScore(Hero hero, Scoring scoring, RoundSpec roundSpec) {
        return switch (scoring) {
            case BALANCED -> totalStats(hero) - statSpread(hero) * 2d
                    + tagModifierScore(List.of(hero), roundSpec) * 10d;
            case BUDGET -> -hero.cost() * 1_000_000d + totalStats(hero);
            case SYNERGY -> tagModifierScore(List.of(hero), roundSpec) * 1_000d + totalStats(hero);
            case AGGRESSIVE -> aggressionStats(hero) * 100d - hero.cost();
        };
    }

    private boolean isIndividuallyEligible(Hero hero, RoundSpec roundSpec) {
        if (hero == null || hero.cost() > roundSpec.budgetCap()) {
            return false;
        }
        if (!isAllowed(hero.role(), roundSpec.allowedRoles())
                || !isAllowed(hero.alignment(), roundSpec.allowedAlignments())
                || !isAllowed(hero.publisher(), roundSpec.allowedPublishers())) {
            return false;
        }
        if (!isAllowed(hero.appearance() == null ? null : hero.appearance().gender(), roundSpec.allowedGenders())
                || !isAllowed(hero.appearance() == null ? null : hero.appearance().race(), roundSpec.allowedRaces())) {
            return false;
        }
        return hasNoBannedTags(hero, roundSpec);
    }

    private boolean isAllowed(String value, List<String> allowedValues) {
        if (allowedValues == null || allowedValues.isEmpty()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalizedValue = normalize(value);
        return allowedValues.stream().map(this::normalize).anyMatch(normalizedValue::equals);
    }

    private boolean hasNoBannedTags(Hero hero, RoundSpec roundSpec) {
        if (roundSpec.bannedTags() == null || roundSpec.bannedTags().isEmpty() || hero.tags() == null) {
            return true;
        }
        Set<String> banned = roundSpec.bannedTags().stream().map(this::normalize).collect(Collectors.toSet());
        return hero.tags().stream().map(this::normalize).noneMatch(banned::contains);
    }

    private boolean exceedsRoleCap(List<Hero> heroes, RoundSpec roundSpec) {
        if (roundSpec.maxSameRole() == null || roundSpec.maxSameRole().isEmpty()) {
            return false;
        }
        return roundSpec.maxSameRole().entrySet().stream()
                .anyMatch(entry -> countRole(heroes, normalize(entry.getKey())) > entry.getValue());
    }

    private boolean meetsRequiredRoles(List<Hero> heroes, RoundSpec roundSpec) {
        return requiredRoles(roundSpec).entrySet().stream()
                .allMatch(entry -> countRole(heroes, entry.getKey()) >= entry.getValue());
    }

    private Map<String, Integer> requiredRoles(RoundSpec roundSpec) {
        if (roundSpec.requiredRoles() == null) {
            return Map.of();
        }
        return roundSpec.requiredRoles().entrySet().stream()
                .collect(Collectors.toMap(entry -> normalize(entry.getKey()), Map.Entry::getValue));
    }

    private long countRole(List<Hero> heroes, String normalizedRole) {
        return heroes.stream().filter(hero -> normalize(hero.role()).equals(normalizedRole)).count();
    }

    private List<Hero> loadAllHeroes() {
        Map<Integer, Hero> heroes = new LinkedHashMap<>();
        for (int page = 0; page < MAX_HERO_PAGES; page++) {
            List<Hero> pageHeroes = arenaApiClient.listHeroes(page, HERO_PAGE_SIZE);
            if (pageHeroes == null || pageHeroes.isEmpty()) {
                break;
            }
            pageHeroes.forEach(hero -> {
                if (hero != null) {
                    heroes.putIfAbsent(hero.id(), hero);
                }
            });
            if (pageHeroes.size() < HERO_PAGE_SIZE) {
                break;
            }
        }
        return List.copyOf(heroes.values());
    }

    private String reasoning(Strategy strategy, RoundSpec roundSpec, SquadState state, int totalCost) {
        return "%s selected deterministically by bounded beam search. Score %.2f, cost %d/%d, map %s. "
                .formatted(
                        strategy.label(),
                        squadScore(state.heroes(), strategy.scoring(), roundSpec),
                        totalCost,
                        roundSpec.budgetCap(),
                        roundSpec.mapType())
                + "Ties are resolved by lower cost, then stable hero ID order.";
    }

    private List<Integer> canonicalIds(List<SquadRecommendation.HeroRef> heroes) {
        return heroes.stream().map(SquadRecommendation.HeroRef::id).sorted().toList();
    }

    private int compareIds(List<Integer> left, List<Integer> right) {
        for (int index = 0; index < Math.min(left.size(), right.size()); index++) {
            int comparison = Integer.compare(left.get(index), right.get(index));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private int totalStats(Hero hero) {
        PowerStats stats = hero.powerstats();
        if (stats == null) {
            return 0;
        }
        return stats.durability() + stats.strength() + stats.power()
                + stats.speed() + stats.intelligence() + stats.combat();
    }

    private int aggressionStats(Hero hero) {
        PowerStats stats = hero.powerstats();
        if (stats == null) {
            return 0;
        }
        return stats.strength() + stats.power() + stats.speed() + stats.combat();
    }

    private int statSpread(Hero hero) {
        PowerStats stats = hero.powerstats();
        if (stats == null) {
            return 0;
        }
        int max = Math.max(Math.max(stats.durability(), stats.strength()),
                Math.max(Math.max(stats.power(), stats.speed()), Math.max(stats.intelligence(), stats.combat())));
        int min = Math.min(Math.min(stats.durability(), stats.strength()),
                Math.min(Math.min(stats.power(), stats.speed()), Math.min(stats.intelligence(), stats.combat())));
        return max - min;
    }

    private int aggregateStatSpread(List<Hero> heroes) {
        if (heroes.isEmpty()) {
            return 0;
        }
        int[] totals = new int[6];
        for (Hero hero : heroes) {
            PowerStats stats = hero.powerstats();
            if (stats == null) {
                continue;
            }
            totals[0] += stats.durability();
            totals[1] += stats.strength();
            totals[2] += stats.power();
            totals[3] += stats.speed();
            totals[4] += stats.intelligence();
            totals[5] += stats.combat();
        }
        int max = java.util.Arrays.stream(totals).max().orElse(0);
        int min = java.util.Arrays.stream(totals).min().orElse(0);
        return max - min;
    }

    private int distinctRoles(List<Hero> heroes) {
        return (int) heroes.stream().map(Hero::role).map(this::normalize).distinct().count();
    }

    private double tagModifierScore(List<Hero> heroes, RoundSpec roundSpec) {
        if (roundSpec.tagModifiers() == null || roundSpec.tagModifiers().isEmpty()) {
            return 0d;
        }
        Map<String, Double> modifiers = roundSpec.tagModifiers().entrySet().stream()
                .collect(Collectors.toMap(entry -> normalize(entry.getKey()), Map.Entry::getValue));
        return heroes.stream()
                .filter(hero -> hero.tags() != null)
                .flatMap(hero -> hero.tags().stream())
                .map(this::normalize)
                .mapToDouble(tag -> modifiers.getOrDefault(tag, 0d))
                .sum();
    }

    private long repeatedTagPairs(List<Hero> heroes) {
        Map<String, Long> counts = heroes.stream()
                .filter(hero -> hero.tags() != null)
                .flatMap(hero -> hero.tags().stream())
                .map(this::normalize)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return counts.values().stream().mapToLong(count -> count * (count - 1) / 2).sum();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private enum Scoring {
        BALANCED,
        BUDGET,
        SYNERGY,
        AGGRESSIVE
    }

    private record Strategy(String id, String label, String summary, Scoring scoring) {
    }

    private record SquadState(List<Hero> heroes, int nextIndex, int cost) {
    }
}

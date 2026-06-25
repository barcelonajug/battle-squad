package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Hero;
import org.barcelonajug.battlecontender.model.RoundSpec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SquadValidationService {

    private final ArenaApiClient arenaApiClient;

    public SquadValidationService(ArenaApiClient arenaApiClient) {
        this.arenaApiClient = arenaApiClient;
    }

    public int validateAndCalculateTotalCost(UUID sessionId, int roundNo, List<Integer> heroIds) {
        RoundSpec roundSpec = arenaApiClient.getRound(roundNo, sessionId);
        List<Hero> heroes = loadHeroes(heroIds);
        List<String> violations = new ArrayList<>();

        validateTeamSize(roundSpec, heroIds, violations);
        validateDistinctHeroes(heroIds, violations);

        int totalCost = heroes.stream().mapToInt(Hero::cost).sum();
        validateBudget(roundSpec, totalCost, violations);
        validateRequiredRoles(roundSpec, heroes, violations);
        validateRoleCaps(roundSpec, heroes, violations);
        validateBannedTags(roundSpec, heroes, violations);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid squad: " + String.join(" ", violations));
        }

        return totalCost;
    }

    public int validateAndCalculateTotalCost(UUID sessionId, int roundNo, SquadRecommendation recommendation) {
        List<Integer> heroIds = recommendation.heroes().stream()
                .map(SquadRecommendation.HeroRef::id)
                .toList();
        return validateAndCalculateTotalCost(sessionId, roundNo, heroIds);
    }

    private List<Hero> loadHeroes(List<Integer> heroIds) {
        if (heroIds == null || heroIds.isEmpty()) {
            return List.of();
        }

        return heroIds.stream()
                .map(arenaApiClient::getHero)
                .toList();
    }

    private void validateTeamSize(RoundSpec roundSpec, List<Integer> heroIds, List<String> violations) {
        int selectedCount = heroIds == null ? 0 : heroIds.size();
        if (selectedCount != roundSpec.teamSize()) {
            violations.add("Expected exactly " + roundSpec.teamSize() + " heroes but got " + selectedCount + ".");
        }
    }

    private void validateDistinctHeroes(List<Integer> heroIds, List<String> violations) {
        if (heroIds == null) {
            return;
        }

        Set<Integer> uniqueIds = new HashSet<>(heroIds);
        if (uniqueIds.size() != heroIds.size()) {
            violations.add("Squads cannot include duplicate heroes.");
        }
    }

    private void validateBudget(RoundSpec roundSpec, int totalCost, List<String> violations) {
        if (totalCost > roundSpec.budgetCap()) {
            violations.add("Budget exceeded: total cost " + totalCost + " is above the cap of " + roundSpec.budgetCap() + ".");
        }
    }

    private void validateRequiredRoles(RoundSpec roundSpec, List<Hero> heroes, List<String> violations) {
        if (roundSpec.requiredRoles() == null || roundSpec.requiredRoles().isEmpty()) {
            return;
        }

        Map<String, Long> roleCounts = countByNormalizedValue(heroes.stream().map(Hero::role).toList());
        roundSpec.requiredRoles().forEach((role, requiredCount) -> {
            long actualCount = roleCounts.getOrDefault(normalize(role), 0L);
            if (actualCount < requiredCount) {
                violations.add("Role requirement not met for " + role + ": need " + requiredCount + " but found " + actualCount + ".");
            }
        });
    }

    private void validateRoleCaps(RoundSpec roundSpec, List<Hero> heroes, List<String> violations) {
        if (roundSpec.maxSameRole() == null || roundSpec.maxSameRole().isEmpty()) {
            return;
        }

        Map<String, Long> roleCounts = countByNormalizedValue(heroes.stream().map(Hero::role).toList());
        roundSpec.maxSameRole().forEach((role, maxCount) -> {
            long actualCount = roleCounts.getOrDefault(normalize(role), 0L);
            if (actualCount > maxCount) {
                violations.add("Role cap exceeded for " + role + ": max " + maxCount + " but found " + actualCount + ".");
            }
        });
    }

    private void validateBannedTags(RoundSpec roundSpec, List<Hero> heroes, List<String> violations) {
        if (roundSpec.bannedTags() == null || roundSpec.bannedTags().isEmpty()) {
            return;
        }

        Set<String> bannedTags = roundSpec.bannedTags().stream()
                .map(this::normalize)
                .collect(Collectors.toSet());

        List<String> matchedTags = new ArrayList<>();
        for (Hero hero : heroes) {
            if (hero.tags() == null) {
                continue;
            }
            for (String tag : hero.tags()) {
                if (bannedTags.contains(normalize(tag))) {
                    matchedTags.add(tag);
                }
            }
        }

        if (!matchedTags.isEmpty()) {
            violations.add("Banned tags present: " + String.join(", ", matchedTags) + ".");
        }
    }

    private Map<String, Long> countByNormalizedValue(List<String> values) {
        Map<String, Long> counts = new HashMap<>();
        for (String value : values) {
            counts.merge(normalize(value), 1L, Long::sum);
        }
        return counts;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

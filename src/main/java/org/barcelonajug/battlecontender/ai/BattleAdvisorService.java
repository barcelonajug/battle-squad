package org.barcelonajug.battlecontender.ai;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BattleAdvisorService {

    private final DraftingSubagentOrchestrator draftingSubagentOrchestrator;
    private final SquadValidationService squadValidationService;

    public BattleAdvisorService(
            DraftingSubagentOrchestrator draftingSubagentOrchestrator,
            SquadValidationService squadValidationService) {
        this.draftingSubagentOrchestrator = draftingSubagentOrchestrator;
        this.squadValidationService = squadValidationService;
    }

    public DraftOptionsResponse buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId) {
        List<SubagentDraft> drafts = draftingSubagentOrchestrator.draft(teamId, sessionId, roundNo);
        List<DraftOption> options = buildValidatedDraftOptions(sessionId, roundNo, drafts);
        DraftOption recommendedOption = chooseRecommendedOption(options);
        return buildDraftOptionsResponse(options, recommendedOption);
    }

    private List<DraftOption> buildValidatedDraftOptions(UUID sessionId, int roundNo, List<SubagentDraft> drafts) {
        Map<String, List<SubagentDraft>> draftsByStrategy = drafts == null
                ? Map.of()
                : drafts.stream()
                        .filter(draft -> draft != null && draft.strategyId() != null)
                        .collect(Collectors.groupingBy(SubagentDraft::strategyId));

        return DraftingStrategies.ALL.stream()
                .map(strategy -> buildValidatedDraftOption(sessionId, roundNo, strategy,
                        draftsByStrategy.getOrDefault(strategy.id(), List.of())))
                .toList();
    }

    private DraftOption buildValidatedDraftOption(UUID sessionId, int roundNo,
            DraftingStrategies.DraftingStrategy strategy, List<SubagentDraft> drafts) {
        if (drafts.size() != 1) {
            String violation = drafts.isEmpty()
                    ? "Subagent did not return a draft."
                    : "Subagent returned %d drafts; expected exactly one.".formatted(drafts.size());
            return invalidOption(strategy, violation);
        }

        SubagentDraft draft = drafts.getFirst();
        if (draft.recommendation() == null) {
            return invalidOption(strategy, hasText(draft.error())
                    ? draft.error()
                    : "Subagent returned a malformed draft.");
        }

        SquadRecommendation recommendation = draft.recommendation();
        SquadValidationService.ValidationResult validation = squadValidationService.validateSquad(sessionId, roundNo,
                recommendation);
        SquadRecommendation normalizedRecommendation = new SquadRecommendation(
                recommendation.heroes(),
                recommendation.strategy(),
                recommendation.reasoning(),
                validation.totalCost());

        return new DraftOption(
                strategy.id(),
                strategy.label(),
                strategy.summary(),
                normalizedRecommendation,
                false,
                validation.valid(),
                validation.violations());
    }

    private DraftOption invalidOption(DraftingStrategies.DraftingStrategy strategy, String violation) {
        SquadRecommendation recommendation = new SquadRecommendation(
                List.of(),
                strategy.label(),
                violation,
                0);
        return new DraftOption(
                strategy.id(),
                strategy.label(),
                strategy.summary(),
                recommendation,
                false,
                false,
                List.of(violation));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private DraftOption chooseRecommendedOption(List<DraftOption> options) {
        return options.stream()
                .filter(DraftOption::valid)
                .min(Comparator.comparingInt(option -> option.recommendation().totalCost()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No drafting strategy produced a valid squad for this round."));
    }

    private DraftOptionsResponse buildDraftOptionsResponse(List<DraftOption> options,
            DraftOption recommendedOption) {
        List<DraftOption> finalizedOptions = options.stream()
                .map(option -> option.strategyId().equals(recommendedOption.strategyId())
                        ? new DraftOption(option.strategyId(), option.label(), option.summary(), option.recommendation(),
                                true, option.valid(), option.violations())
                        : option)
                .toList();

        return new DraftOptionsResponse(finalizedOptions, recommendedOption.strategyId());
    }

}

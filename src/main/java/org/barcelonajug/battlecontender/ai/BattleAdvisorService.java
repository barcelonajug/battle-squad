package org.barcelonajug.battlecontender.ai;

import org.barcelonajug.battlecontender.ai.tools.ArenaManagementTool;
import org.barcelonajug.battlecontender.ai.tools.HeroSearchTool;
import org.barcelonajug.battlecontender.ai.tools.TeamHeroSearchTool;
import org.barcelonajug.battlecontender.client.ArenaApiClient;
import org.barcelonajug.battlecontender.model.Team;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.core.Ordered;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BattleAdvisorService {
    private static final Logger logger = LoggerFactory.getLogger(BattleAdvisorService.class);
    private static final double MIN_CONFIDENCE = 0.5;
    private static final long DRAFT_TIMEOUT_SECONDS = 90;
    private static final long EVALUATION_TIMEOUT_SECONDS = 30;

    private static final String SYSTEM_PROMPT = """
            You are the Battle Contender squad optimizer.

            Use the provided tools to inspect the active session, the round constraints, and the available heroes.
            Use findHeroesForRound before drafting. It returns a team-specific sample from the wider
            eligible hero pool. Choose only hero IDs from that sample so teams explore different squads.
            Compare candidates against budget, team size, roles, banned tags, and allowed attributes.
            Keep the final reasoning field concise and readable for the UI.
            Return only a SquadRecommendation with valid structured output.
            """;

    private static final List<DraftingStrategy> STRATEGIES = List.of(
            new DraftingStrategy(
                    "balanced-drafter",
                    "Balanced Draft",
                    "General-purpose, low-risk composition.",
                    """
                            Draft a balanced, low-risk squad.
                            Prioritize role coverage first, then fit under budget, then general quality.
                            Avoid fragile tradeoffs unless they materially improve compliance.
                            """),
            new DraftingStrategy(
                    "budget-drafter",
                    "Budget Saver",
                    "Minimize spend while staying valid.",
                    """
                            Draft the cheapest valid squad you can assemble.
                            Lock required roles first, preserve budget headroom, and prefer lower-cost substitutes.
                            Reason explicitly about spare budget remaining after the draft.
                            """),
            new DraftingStrategy(
                    "synergy-drafter",
                    "Synergy Draft",
                    "Lean into tags and role interactions allowed by the round.",
                    """
                            Draft a synergy-focused squad.
                            Favor complementary tags, coherent role interactions, and round modifiers when available.
                            If synergy conflicts with hard constraints, satisfy the constraints first.
                            """),
            new DraftingStrategy(
                    "aggressive-drafter",
                    "Aggressive Draft",
                    "Bias toward offensive pressure and higher-impact picks.",
                    """
                            Draft an aggressive squad.
                            Prioritize offensive pressure and high-impact picks while still staying fully valid.
                            If you must trade off between aggression and legality, legality wins.
                            """));

    private final HeroSearchTool heroSearchTool;
    private final ArenaManagementTool arenaManagementTool;
    private final SquadValidationService squadValidationService;
    private final SquadEvaluator squadEvaluator;
    private final ArenaApiClient arenaApiClient;
    private final ChatClient.Builder chatClientBuilder;
    private final Executor draftingExecutor;
    private final long draftTimeoutMillis;
    private final long evaluationTimeoutMillis;

    @Autowired
    public BattleAdvisorService(ChatClient.Builder chatClientBuilder,
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool,
            SquadValidationService squadValidationService,
            SquadEvaluator squadEvaluator,
            ArenaApiClient arenaApiClient) {
        this(chatClientBuilder, heroSearchTool, arenaManagementTool, squadValidationService, squadEvaluator, arenaApiClient,
                command -> Thread.startVirtualThread(command),
                TimeUnit.SECONDS.toMillis(DRAFT_TIMEOUT_SECONDS),
                TimeUnit.SECONDS.toMillis(EVALUATION_TIMEOUT_SECONDS));
    }

    BattleAdvisorService(ChatClient.Builder chatClientBuilder,
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool,
            SquadValidationService squadValidationService,
            SquadEvaluator squadEvaluator,
            ArenaApiClient arenaApiClient,
            Executor draftingExecutor) {
        this(chatClientBuilder, heroSearchTool, arenaManagementTool, squadValidationService, squadEvaluator, arenaApiClient,
                draftingExecutor, TimeUnit.SECONDS.toMillis(DRAFT_TIMEOUT_SECONDS),
                TimeUnit.SECONDS.toMillis(EVALUATION_TIMEOUT_SECONDS));
    }

    BattleAdvisorService(ChatClient.Builder chatClientBuilder,
            HeroSearchTool heroSearchTool,
            ArenaManagementTool arenaManagementTool,
            SquadValidationService squadValidationService,
            SquadEvaluator squadEvaluator,
            ArenaApiClient arenaApiClient,
            Executor draftingExecutor,
            long draftTimeoutMillis,
            long evaluationTimeoutMillis) {
        this.chatClientBuilder = chatClientBuilder;
        this.heroSearchTool = heroSearchTool;
        this.arenaManagementTool = arenaManagementTool;
        this.squadValidationService = squadValidationService;
        this.squadEvaluator = squadEvaluator;
        this.arenaApiClient = arenaApiClient;
        this.draftingExecutor = draftingExecutor;
        this.draftTimeoutMillis = draftTimeoutMillis;
        this.evaluationTimeoutMillis = evaluationTimeoutMillis;
    }

    public DraftOptionsResponse buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId) {
        return buildOptimalSquad(teamId, roundNo, sessionId, progress -> {});
    }

    public DraftOptionsResponse buildOptimalSquad(UUID teamId, int roundNo, UUID sessionId,
            Consumer<OptimizationProgress> progressListener) {
        AtomicInteger drafted = new AtomicInteger();
        AtomicInteger reviewed = new AtomicInteger();
        AtomicReference<RuntimeException> firstDraftFailure = new AtomicReference<>();
        Object progressLock = new Object();
        List<CompletableFuture<DraftOption>> futures = STRATEGIES.stream()
                .map(strategy -> CompletableFuture
                        .supplyAsync(() -> buildValidatedDraftOption(teamId, roundNo, sessionId, strategy), draftingExecutor)
                        .orTimeout(draftTimeoutMillis, TimeUnit.MILLISECONDS)
                        .handle((draft, failure) -> {
                            synchronized (progressLock) {
                                drafted.incrementAndGet();
                                reportProgress(progressListener, drafted.get(), reviewed.get());
                            }
                            if (failure == null) return draft;
                            RuntimeException cause = draftFailure(failure);
                            logger.warn("Drafting strategy {} failed: {}", strategy.id(), cause.toString());
                            firstDraftFailure.compareAndSet(null, cause);
                            return failedDraftOption(strategy);
                        })
                        .thenCompose(draft -> CompletableFuture
                                .supplyAsync(() -> evaluateValidOption(sessionId, roundNo, draft), draftingExecutor)
                                .completeOnTimeout(draft.valid()
                                                ? withEvaluation(draft, null, null, "UNAVAILABLE") : draft,
                                        evaluationTimeoutMillis, TimeUnit.MILLISECONDS)
                                .exceptionally(failure -> {
                                    logger.warn("Review for strategy {} failed: {}", strategy.id(), failure.toString());
                                    return draft.valid() ? withEvaluation(draft, null, null, "UNAVAILABLE") : draft;
                                }))
                        .handle((option, failure) -> {
                            synchronized (progressLock) {
                                reviewed.incrementAndGet();
                                reportProgress(progressListener, drafted.get(), reviewed.get());
                            }
                            if (failure == null) return option;
                            logger.warn("Review for strategy {} failed: {}", strategy.id(), failure.toString());
                            return failedDraftOption(strategy);
                        }))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RuntimeException cause) throw cause;
            throw ex;
        }
        List<DraftOption> evaluatedOptions = futures.stream().map(CompletableFuture::join).toList();
        if (evaluatedOptions.stream().allMatch(option -> "DRAFT_FAILED".equals(option.evaluationStatus()))) {
            RuntimeException failure = firstDraftFailure.get();
            throw failure == null ? new IllegalStateException("All drafting strategies failed.") : failure;
        }
        String preferredStrategyId = preferredStrategyId(teamId, sessionId);
        DraftOption preferredOption = evaluatedOptions.stream()
                .filter(DraftOption::valid)
                .filter(option -> option.strategyId().equals(preferredStrategyId))
                .findFirst().orElse(null);
        if (preferredOption != null) {
            return buildDraftOptionsResponse(evaluatedOptions, preferredOption, "TEAM_STRATEGY");
        }
        boolean useJev = evaluatedOptions.stream()
                .anyMatch(option -> option.valid() && "EVALUATED".equals(option.evaluationStatus()));
        DraftOption recommendedOption = chooseRecommendedOption(evaluatedOptions, useJev);
        return buildDraftOptionsResponse(evaluatedOptions, recommendedOption,
                useJev ? "JEV" : "COST_FALLBACK");
    }

    private String preferredStrategyId(UUID teamId, UUID sessionId) {
        try {
            List<Team> teams = arenaApiClient.listTeams(sessionId);
            if (teams == null) return null;
            List<UUID> teamIds = teams.stream()
                    .sorted(Comparator.comparing(Team::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Team::teamId))
                    .map(Team::teamId).toList();
            int index = teamIds.indexOf(teamId);
            return index < 0 ? null : STRATEGIES.get(index % STRATEGIES.size()).id();
        } catch (RuntimeException ex) {
            logger.warn("Could not assign a team strategy: {}", ex.toString());
            return null;
        }
    }

    private RuntimeException draftFailure(Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause() : failure;
        return cause instanceof RuntimeException runtime ? runtime : new IllegalStateException(cause);
    }

    private void reportProgress(Consumer<OptimizationProgress> listener, int drafted, int reviewed) {
        try {
            listener.accept(new OptimizationProgress(drafted, reviewed, STRATEGIES.size()));
        } catch (RuntimeException ex) {
            logger.debug("Optimization progress listener failed", ex);
        }
    }

    private DraftOption buildValidatedDraftOption(UUID teamId, int roundNo, UUID sessionId,
            DraftingStrategy strategy) {
        String conversationId = "%s:%s:%d:%s".formatted(teamId, sessionId, roundNo, strategy.id());
        ChatClient chatClient = buildStrategyClient(teamId, strategy);

        SquadRecommendation recommendation = chatClient.prompt()
                .system(SYSTEM_PROMPT + "\n\nStrategy profile:\n" + strategy.instructions())
                .user("""
                        Optimize a battle squad for team %s.
                        Session: %s
                        Round: %d

                        Strategy profile: %s
                        Start with findHeroesForRound for constrained candidate discovery.
                        """.formatted(teamId, sessionId, roundNo, strategy.label()))
                .advisors(advisors -> advisors.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(SquadRecommendation.class);

        SquadValidationService.ValidationResult validation = null;
        RuntimeException validationError = null;
        try {
            validation = squadValidationService.validateSquad(sessionId, roundNo, recommendation);
        } catch (RuntimeException ex) {
            logger.warn("Could not validate model draft for strategy {}: {}", strategy.id(), ex.toString());
            validationError = ex;
        }

        List<HeroSearchTool.HeroSummary> candidates = heroSearchTool.findHeroesForTeamRound(
                teamId, sessionId, roundNo, strategy.id());
        if (candidates != null && !candidates.isEmpty()) {
            Set<Integer> candidateIds = candidates.stream().map(HeroSearchTool.HeroSummary::id)
                    .collect(java.util.stream.Collectors.toSet());
            boolean usedTeamSample = recommendation.heroes() != null && recommendation.heroes().stream()
                    .allMatch(hero -> candidateIds.contains(hero.id()));
            if (validation == null || !validation.valid() || !usedTeamSample) {
                List<HeroSearchTool.HeroSummary> baseline = heroSearchTool.buildTeamBaseline(
                        teamId, sessionId, roundNo, strategy.id());
                if (baseline != null && !baseline.isEmpty()) {
                    SquadRecommendation replacement = new SquadRecommendation(
                            baseline.stream().map(hero -> new SquadRecommendation.HeroRef(hero.id(), hero.name())).toList(),
                            strategy.label(),
                            "Verified alternative from this team's round-eligible hero sample.",
                            0);
                    SquadValidationService.ValidationResult replacementValidation = squadValidationService.validateSquad(
                            sessionId, roundNo, replacement);
                    if (replacementValidation.valid()) {
                        logger.info("Using verified team-specific alternative for strategy {}", strategy.id());
                        recommendation = replacement;
                        validation = replacementValidation;
                    }
                }
            }
        }
        if (validation == null) {
            throw validationError == null
                    ? new IllegalArgumentException("The draft could not be validated for this round.")
                    : validationError;
        }
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
                validation.violations(),
                null,
                null,
                "NOT_EVALUATED");
    }

    private DraftOption evaluateValidOption(UUID sessionId, int roundNo, DraftOption option) {
        if (!option.valid()) {
            return option;
        }
        try {
            SquadEvaluator.Evaluation evaluation = squadEvaluator.evaluate(sessionId, roundNo, option);
            boolean usable = Double.isFinite(evaluation.score()) && evaluation.score() >= 0
                    && evaluation.score() <= 100 && Double.isFinite(evaluation.confidence())
                    && evaluation.confidence() >= MIN_CONFIDENCE && evaluation.confidence() <= 1;
            return withEvaluation(option,
                    Double.isFinite(evaluation.score()) ? evaluation.score() : null,
                    Double.isFinite(evaluation.confidence()) ? evaluation.confidence() : null,
                    usable ? "EVALUATED" : "LOW_CONFIDENCE");
        } catch (RuntimeException ex) {
            logger.warn("TypeSafe evaluation unavailable for strategy {}: {}", option.strategyId(), ex.getMessage());
            return withEvaluation(option, null, null, "UNAVAILABLE");
        }
    }

    private DraftOption withEvaluation(DraftOption option, Double score, Double confidence, String status) {
        return new DraftOption(option.strategyId(), option.label(), option.summary(), option.recommendation(),
                option.recommended(), option.valid(), option.violations(), score, confidence, status);
    }

    private DraftOption failedDraftOption(DraftingStrategy strategy) {
        return new DraftOption(strategy.id(), strategy.label(), strategy.summary(),
                new SquadRecommendation(List.of(), strategy.label(), "This draft could not be produced.", 0),
                false, false, List.of("Drafting failed. Retry optimization to generate another option."),
                null, null, "DRAFT_FAILED");
    }

    private DraftOption chooseRecommendedOption(List<DraftOption> options, boolean useJev) {
        Comparator<DraftOption> byCost = Comparator.comparingInt((DraftOption option) -> option.recommendation().totalCost())
                .thenComparing(DraftOption::strategyId);
        Comparator<DraftOption> ranking = useJev
                ? Comparator.comparingDouble((DraftOption option) -> -option.evaluationScore()).thenComparing(byCost)
                : byCost;
        return options.stream()
                .filter(DraftOption::valid)
                .filter(option -> !useJev || "EVALUATED".equals(option.evaluationStatus()))
                .min(ranking)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No drafting strategy produced a valid squad for this round."));
    }

    private DraftOptionsResponse buildDraftOptionsResponse(List<DraftOption> options,
            DraftOption recommendedOption, String selectionMethod) {
        List<DraftOption> finalizedOptions = options.stream()
                .map(option -> option.strategyId().equals(recommendedOption.strategyId())
                        ? new DraftOption(option.strategyId(), option.label(), option.summary(), option.recommendation(),
                                true, option.valid(), option.violations(), option.evaluationScore(),
                                option.evaluationConfidence(), option.evaluationStatus())
                        : option)
                .toList();

        return new DraftOptionsResponse(finalizedOptions, recommendedOption.strategyId(), selectionMethod);
    }

    private ChatClient buildStrategyClient(UUID teamId, DraftingStrategy strategy) {
        return chatClientBuilder.clone()
                .defaultTools(new TeamHeroSearchTool(heroSearchTool, teamId, strategy.id()), arenaManagementTool)
                .defaultAdvisors(
                        ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build())
                                .order(Ordered.HIGHEST_PRECEDENCE + 1000)
                                .build())
                .build();
    }

    private record DraftingStrategy(String id, String label, String summary, String instructions) {
    }

    public record OptimizationProgress(int draftsCompleted, int reviewsCompleted, int total) {
    }
}

package com.joe_bor.svt_api.services.action;

import com.joe_bor.svt_api.controllers.action.dto.SubmitActionRequest;
import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.SpecialEffectType;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.services.economy.EconomyService;
import com.joe_bor.svt_api.services.economy.PassiveDrainService;
import com.joe_bor.svt_api.services.game.GameSessionService;
import com.joe_bor.svt_api.services.game.WinLossEvaluator;
import com.joe_bor.svt_api.services.route.RouteService;
import com.joe_bor.svt_api.services.turn.PendingEventService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionService {

    private static final long BUG_CRISIS_EVENT_ID = 13L;
    private static final long MUTINY_RANDOM_CHOICE_ID = 14L;
    private static final long SAND_HILL_RANDOM_CHOICE_ID = 21L;
    private static final long ACQUI_HIRE_EVENT_ID = 7L;
    private static final long ACQUI_HIRE_CHOICE_ID = 7L;
    private static final long LINKEDIN_EVENT_ID = 14L;
    private static final long LINKEDIN_GAME_OVER_CHOICE_ID = 15L;
    private static final TurnResolutionSummaryDto.StatDeltas ZERO_STAT_DELTAS =
            new TurnResolutionSummaryDto.StatDeltas(0, 0, 0, 0);

    private final GameSessionService gameSessionService;
    private final PendingEventService pendingEventService;
    private final ActionSubmissionValidator actionSubmissionValidator;
    private final EconomyService economyService;
    private final PassiveDrainService passiveDrainService;
    private final ActionAffordabilityService actionAffordabilityService;
    private final ActionEffectService actionEffectService;
    private final WinLossEvaluator winLossEvaluator;
    private final RouteService routeService;

    @Transactional
    public GameStateDto resolveAction(UUID gameId, SubmitActionRequest request) {
        GameSessionEntity session = gameSessionService.getGameSession(gameId);
        List<EventEntity> pendingEvents = pendingEventService.loadPendingEventEntities(session.getPendingEventIds());
        ActionSubmissionValidator.ValidatedActionSubmission validated =
                actionSubmissionValidator.validate(session, request, pendingEvents);

        // A turn always resolves in the same player-facing order: events first, then passives, then one action.
        List<TurnResolutionSummaryDto.EventResolutionDetail> eventResolutions = new ArrayList<>();
        for (EventEntity event : pendingEvents) {
            TurnResolutionSummaryDto.EventResolutionDetail resolution = event.isHasChoice()
                    ? applyChoiceEvent(session, event, validated.selectedChoicesByEventId().get(event.getId()))
                    : applyAutoEvent(session, event);
            eventResolutions.add(resolution);

            if (resolution.triggeredGameOver()) {
                // Some event choices end the run immediately, so the player action is ignored for that turn.
                session.getPendingEventIds().clear();
                return gameSessionService.toDto(session, new TurnResolutionSummaryDto(
                        eventResolutions,
                        null,
                        null,
                        new TurnResolutionSummaryDto.WinLossResult(true, session.getGameEndReason())
                ));
            }
        }

        int cashFromEconomy = economyService.applyPassiveEconomy(session);
        Integer cryptoSettlement = normalizeNullable(economyService.applyCryptoSettlement(session));
        int coffeeDecay = passiveDrainService.applyCoffeeDecay(session);
        TurnResolutionSummaryDto.PassiveDeltas passiveDeltas = new TurnResolutionSummaryDto.PassiveDeltas(
                cashFromEconomy,
                coffeeDecay,
                ZERO_STAT_DELTAS,
                cryptoSettlement
        );

        actionAffordabilityService.validateActionLegal(
                session,
                request.action().type(),
                validated.hasLoseActionPending()
        );

        TurnResolutionSummaryDto.ActionResolutionDetail actionResolution = actionEffectService.applyAction(
                session,
                request.action(),
                routeService.getAvailableNextLocations(session.getCurrentLocation())
        );

        GameEndReason reason = winLossEvaluator.evaluateAndApply(session);
        session.getPendingEventIds().clear();

        return gameSessionService.toDto(session, new TurnResolutionSummaryDto(
                eventResolutions,
                passiveDeltas,
                actionResolution,
                new TurnResolutionSummaryDto.WinLossResult(reason != null, reason)
        ));
    }

    // Applies no-choice events exactly as the turn resolver would narrate them to the player.
    private TurnResolutionSummaryDto.EventResolutionDetail applyAutoEvent(GameSessionEntity session, EventEntity event) {
        VisibleStatsSnapshot before = VisibleStatsSnapshot.capture(session);
        String dynamicNote = null;

        if (event.getId() == BUG_CRISIS_EVENT_ID) {
            // Bug Crisis cashes in every unresolved bug as churn, then resets the incident back to zero.
            int lostCustomers = Math.max(0, session.getBugs());
            session.setCustomers(session.getCustomers() - lostCustomers);
            session.setBugs(0);
            dynamicNote = "Lost " + lostCustomers + " customers, bugs reset";
        } else {
            session.setCash(session.getCash() + event.getAutoCashEffect());
            session.setCustomers(session.getCustomers() + event.getAutoCustomersEffect());
            session.setMorale(session.getMorale() + event.getAutoMoraleEffect());
            session.setCoffee(session.getCoffee() + event.getAutoCoffeeEffect());
            session.setBugs(session.getBugs() + event.getAutoBugsEffect());
        }

        clampSession(session);
        VisibleStatsSnapshot after = VisibleStatsSnapshot.capture(session);

        return new TurnResolutionSummaryDto.EventResolutionDetail(
                event.getId(),
                null,
                after.deltaFrom(before),
                false,
                dynamicNote
        );
    }

    // Applies the player's selected event branch, including special effects owned in Java.
    private TurnResolutionSummaryDto.EventResolutionDetail applyChoiceEvent(
            GameSessionEntity session,
            EventEntity event,
            EventChoiceEntity choice
    ) {
        VisibleStatsSnapshot before = VisibleStatsSnapshot.capture(session);
        boolean triggeredGameOver = false;
        String dynamicNote = null;

        session.setCash(session.getCash() + choice.getCashEffect());
        session.setCustomers(session.getCustomers() + choice.getCustomersEffect());
        session.setMorale(session.getMorale() + choice.getMoraleEffect());
        session.setCoffee(session.getCoffee() + choice.getCoffeeEffect());
        session.setBugs(session.getBugs() + choice.getBugsEffect());

        if (choice.getSpecialEffect() == SpecialEffectType.GAME_OVER) {
            triggeredGameOver = true;
            if (event.getId() == ACQUI_HIRE_EVENT_ID && choice.getId() == ACQUI_HIRE_CHOICE_ID) {
                session.setGameEndReason(GameEndReason.ACQUIRED);
                dynamicNote = "Accepted Acqui-hire Offer";
            } else if (event.getId() == LINKEDIN_EVENT_ID && choice.getId() == LINKEDIN_GAME_OVER_CHOICE_ID) {
                session.setGameEndReason(GameEndReason.TOOK_LINKEDIN_JOB);
                dynamicNote = "Accepted LinkedIn job";
            }
            session.setStatus(GameSessionStatus.LOST);
        } else if (choice.getSpecialEffect() == SpecialEffectType.RANDOM_5050) {
            // The seed data points to "coin flip" choices, but the actual outcomes are game-rule owned here.
            boolean positive = ThreadLocalRandom.current().nextBoolean();
            if (choice.getId() == MUTINY_RANDOM_CHOICE_ID) {
                session.setMorale(session.getMorale() + (positive ? 15 : -10));
            } else if (choice.getId() == SAND_HILL_RANDOM_CHOICE_ID) {
                if (positive) {
                    session.setCash(session.getCash() + 4_000);
                    session.setCustomers(session.getCustomers() + 4);
                } else {
                    session.setMorale(session.getMorale() - 15);
                }
            }
            dynamicNote = "50/50 resolved " + (positive ? "positive" : "negative");
        } else if (choice.getSpecialEffect() == SpecialEffectType.LINKEDIN_BONUS) {
            session.setLinkedinBonusActive(true);
            dynamicNote = "LinkedIn bonus activated";
        }

        clampSession(session);
        VisibleStatsSnapshot after = VisibleStatsSnapshot.capture(session);

        return new TurnResolutionSummaryDto.EventResolutionDetail(
                event.getId(),
                choice.getId(),
                after.deltaFrom(before),
                triggeredGameOver,
                dynamicNote
        );
    }

    // Keeps "no settlement happened" distinct from "a zero-dollar settlement happened" in the response DTO.
    private static Integer normalizeNullable(int value) {
        return value == 0 ? null : value;
    }

    // Turn resolution can stack several deltas, so clamp once before we publish the updated state.
    private static void clampSession(GameSessionEntity session) {
        session.setMorale(Math.max(0, Math.min(100, session.getMorale())));
        session.setCoffee(Math.max(0, session.getCoffee()));
        session.setCustomers(Math.max(0, session.getCustomers()));
        session.setBugs(Math.max(0, session.getBugs()));
    }

    private record VisibleStatsSnapshot(
            int cash,
            int customers,
            int morale,
            int coffee
    ) {
        // Captures only the player-visible stats that feed lastResolution deltas.
        private static VisibleStatsSnapshot capture(GameSessionEntity session) {
            return new VisibleStatsSnapshot(
                    session.getCash(),
                    session.getCustomers(),
                    session.getMorale(),
                    session.getCoffee()
            );
        }

        // Converts before/after visible stats into the API delta shape used by resolution summaries.
        private TurnResolutionSummaryDto.StatDeltas deltaFrom(VisibleStatsSnapshot before) {
            return new TurnResolutionSummaryDto.StatDeltas(
                    cash - before.cash(),
                    customers - before.customers(),
                    morale - before.morale(),
                    coffee - before.coffee()
            );
        }
    }
}

package com.joe_bor.svt_api.services.action;

import com.joe_bor.svt_api.controllers.action.dto.SubmitActionRequest;
import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.services.economy.EconomyService;
import com.joe_bor.svt_api.services.economy.PassiveDrainService;
import com.joe_bor.svt_api.services.game.GameSessionService;
import com.joe_bor.svt_api.services.game.WinLossEvaluator;
import com.joe_bor.svt_api.services.route.RouteService;
import com.joe_bor.svt_api.services.weather.WeatherModifierService;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
import com.joe_bor.svt_api.services.weather.WeatherTimelineService;
import com.joe_bor.svt_api.services.turn.EventEffectApplier;
import com.joe_bor.svt_api.services.turn.PendingEventService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionService {

    private static final TurnResolutionSummaryDto.TemperatureModifier ZERO_TEMPERATURE_MODIFIER =
            new TurnResolutionSummaryDto.TemperatureModifier(0, 0);

    private final GameSessionService gameSessionService;
    private final PendingEventService pendingEventService;
    private final ActionSubmissionValidator actionSubmissionValidator;
    private final EconomyService economyService;
    private final PassiveDrainService passiveDrainService;
    private final ActionAffordabilityService actionAffordabilityService;
    private final ActionEffectService actionEffectService;
    private final EventEffectApplier eventEffectApplier;
    private final WinLossEvaluator winLossEvaluator;
    private final RouteService routeService;
    private final WeatherTimelineService weatherTimelineService;
    private final WeatherModifierService weatherModifierService;

    @Transactional
    public GameStateDto resolveAction(UUID gameId, SubmitActionRequest request) {
        var session = gameSessionService.getGameSession(gameId);
        List<EventEntity> pendingEvents = pendingEventService.loadPendingEventEntities(session.getPendingEventIds());
        WeatherSnapshot weather = weatherTimelineService.getWeather(
                session.getCurrentLocation().getLatitude(),
                session.getCurrentLocation().getLongitude(),
                session.getCurrentGameDate()
        );

        // 1. Validate the submitted turn package.
        ActionSubmissionValidator.ValidatedActionSubmission validated =
                actionSubmissionValidator.validate(session, request, pendingEvents);

        // 2. Resolve pending events.
        List<TurnResolutionSummaryDto.EventResolutionDetail> eventResolutions = new ArrayList<>();
        for (EventEntity event : pendingEvents) {
            TurnResolutionSummaryDto.EventResolutionDetail resolution = event.isHasChoice()
                    ? eventEffectApplier.applyChoiceEvent(
                            session,
                            event,
                            validated.selectedChoicesByEventId().get(event.getId())
                    )
                    : eventEffectApplier.applyAutoEvent(session, event);
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

        // 3. Apply passive turn effects.
        // Passive systems tick after events so they reflect any turn-opening fallout before the player acts.
        int cashFromEconomy = economyService.applyPassiveEconomy(session);
        Integer cryptoSettlement = normalizeNullable(economyService.applyCryptoSettlement(session));
        int coffeeDecay = passiveDrainService.applyCoffeeDecay(session, weather.bucket());
        TurnResolutionSummaryDto.TemperatureModifier temperatureModifier = applyTemperatureModifier(
                session,
                weatherModifierService.computeTemperatureModifier(weather.temperatureBracket())
        );
        TurnResolutionSummaryDto.PassiveDeltas passiveDeltas = new TurnResolutionSummaryDto.PassiveDeltas(
                cashFromEconomy,
                coffeeDecay,
                temperatureModifier,
                cryptoSettlement
        );

        // Recheck legality after passives because coffee decay or event fallout can invalidate an otherwise valid plan.
        actionAffordabilityService.validateActionLegal(
                session,
                request.action().type(),
                validated.hasLoseActionPending(),
                weather
        );

        // 4. Execute one player action.
        TurnResolutionSummaryDto.ActionResolutionDetail actionResolution = actionEffectService.applyAction(
                session,
                request.action(),
                routeService.getAvailableNextLocations(session.getCurrentLocation()),
                weather
        );

        // 5. Evaluate win/loss and return the updated state.
        // Win/loss is evaluated last so the response reflects the final state after the whole turn settles.
        GameEndReason reason = winLossEvaluator.evaluateAndApply(session);
        session.getPendingEventIds().clear();

        return gameSessionService.toDto(session, new TurnResolutionSummaryDto(
                eventResolutions,
                passiveDeltas,
                actionResolution,
                new TurnResolutionSummaryDto.WinLossResult(reason != null, reason)
        ));
    }

    // Keeps "no settlement happened" distinct from "a zero-dollar settlement happened" in the response DTO.
    private static Integer normalizeNullable(int value) {
        return value == 0 ? null : value;
    }

    private static TurnResolutionSummaryDto.TemperatureModifier applyTemperatureModifier(
            com.joe_bor.svt_api.models.session.GameSessionEntity session,
            TurnResolutionSummaryDto.TemperatureModifier modifier
    ) {
        int startingMorale = session.getMorale();
        int startingCoffee = session.getCoffee();
        session.setMorale(Math.max(0, Math.min(100, session.getMorale() + modifier.morale())));
        session.setCoffee(Math.max(0, session.getCoffee() + modifier.coffee()));
        return new TurnResolutionSummaryDto.TemperatureModifier(
                session.getMorale() - startingMorale,
                session.getCoffee() - startingCoffee
        );
    }
}

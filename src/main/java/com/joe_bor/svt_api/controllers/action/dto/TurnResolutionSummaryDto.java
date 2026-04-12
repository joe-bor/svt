package com.joe_bor.svt_api.controllers.action.dto;

import com.joe_bor.svt_api.models.gameplay.ActionType;
import com.joe_bor.svt_api.models.session.GameEndReason;
import java.util.List;

/**
 * Summarizes the ordered turn-resolution pipeline for action responses.
 */
public record TurnResolutionSummaryDto(
        List<EventResolutionDetail> eventResolutions,
        PassiveDeltas passiveDeltas,
        ActionResolutionDetail actionResolution,
        WinLossResult winLoss
) {
    public record EventResolutionDetail(
            long eventId,
            Long choiceId,
            StatDeltas statDeltas,
            boolean triggeredGameOver,
            String dynamicNote
    ) {
    }

    public record StatDeltas(
            int cash,
            int customers,
            int morale,
            int coffee
    ) {
    }

    public record PassiveDeltas(
            int cashFromEconomy,
            int coffeeDecay,
            TemperatureModifier temperatureModifier,
            Integer cryptoSettlementCredited
    ) {
    }

    public record TemperatureModifier(
            int morale,
            int coffee
    ) {
    }

    public record ActionResolutionDetail(
            ActionType actionType,
            int cashDelta,
            int coffeeDelta,
            int moraleDelta,
            int customersDelta,
            Long destinationLocationId,
            WeatherSurcharges weatherSurcharges,
            String detourBonusApplied,
            List<String> notes
    ) {
    }

    public record WeatherSurcharges(
            int coffee
    ) {
    }

    public record WinLossResult(
            boolean ended,
            GameEndReason reason
    ) {
    }
}

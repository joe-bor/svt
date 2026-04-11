package com.joe_bor.svt_api.controllers.game.dto;

import com.joe_bor.svt_api.models.gameplay.ActionType;

public record AvailableActionDto(
        ActionType type,
        int cashCost,
        int coffeeCost,
        int moraleCost,
        WeatherSurcharge weatherSurcharge,
        boolean requiresDestination,
        boolean requiresAmount,
        Integer minAmount,
        Integer maxAmount,
        String disabledReason
) {

    public record WeatherSurcharge(
            int coffeeAdded,
            int cashAdded,
            int moraleAdded
    ) {
    }
}

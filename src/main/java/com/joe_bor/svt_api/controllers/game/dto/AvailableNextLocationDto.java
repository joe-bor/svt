package com.joe_bor.svt_api.controllers.game.dto;

public record AvailableNextLocationDto(
        long locationId,
        String name,
        boolean detour,
        RouteType routeType,
        int eta,
        String detourBonusStat,
        Integer detourBonusValue
) {
}

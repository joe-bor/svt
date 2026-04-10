package com.joe_bor.svt_api.controllers.game.dto;

public record AvailableNextLocationDto(
        long locationId,
        String name,
        boolean detour,
        int eta,
        String detourBonusStat,
        Integer detourBonusValue
) {
}

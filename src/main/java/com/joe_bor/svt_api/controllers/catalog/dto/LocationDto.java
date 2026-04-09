package com.joe_bor.svt_api.controllers.catalog.dto;

public record LocationDto(
        Long id,
        String name,
        String description,
        Short routeOrder,
        boolean detour,
        Long branchesFromId,
        double latitude,
        double longitude,
        String detourBonusStat,
        Integer detourBonusValue
) {
}

package com.joe_bor.svt_api.services;

import com.joe_bor.svt_api.controllers.catalog.dto.LocationDto;
import com.joe_bor.svt_api.models.location.LocationEntity;

public final class LocationDtoMapper {

    private LocationDtoMapper() {
    }

    public static LocationDto toLocationDto(LocationEntity location) {
        return new LocationDto(
                location.getId(),
                location.getName(),
                location.getDescription(),
                location.getRouteOrder(),
                location.isDetour(),
                location.getBranchesFrom() != null ? location.getBranchesFrom().getId() : null,
                location.getLatitude(),
                location.getLongitude(),
                location.getDetourBonusStat() != null ? location.getDetourBonusStat().name() : null,
                location.getDetourBonusValue()
        );
    }
}

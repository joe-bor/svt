package com.joe_bor.svt_api.services.route;

import com.joe_bor.svt_api.controllers.game.dto.AvailableNextLocationDto;
import com.joe_bor.svt_api.controllers.game.dto.RouteType;
import com.joe_bor.svt_api.models.location.LocationEntity;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private static final int TERMINAL_ROUTE_ORDER = 10;

    private final LocationRepository locationRepository;

    public List<AvailableNextLocationDto> getAvailableNextLocations(LocationEntity currentLocation) {
        if (currentLocation.isDetour()) {
            LocationEntity branchPoint = currentLocation.getBranchesFrom();
            short rejoinOrder = (short) (branchPoint.getRouteOrder() + 1);

            return locationRepository.findByRouteOrder(rejoinOrder)
                    .map(this::toDto)
                    .map(List::of)
                    .orElse(List.of());
        }

        List<AvailableNextLocationDto> results = new ArrayList<>();
        short nextOrder = (short) (currentLocation.getRouteOrder() + 1);

        locationRepository.findByRouteOrder(nextOrder)
                .map(this::toDto)
                .ifPresent(results::add);

        locationRepository.findAllByBranchesFromOrderByIdAsc(currentLocation)
                .stream()
                .map(this::toDto)
                .forEach(results::add);

        return List.copyOf(results);
    }

    private AvailableNextLocationDto toDto(LocationEntity location) {
        int effectiveOrder = location.isDetour()
                ? location.getBranchesFrom().getRouteOrder()
                : location.getRouteOrder();

        return new AvailableNextLocationDto(
                location.getId(),
                location.getName(),
                location.isDetour(),
                location.isDetour() ? RouteType.DETOUR : RouteType.MAIN_ROUTE,
                TERMINAL_ROUTE_ORDER - effectiveOrder + 1,
                location.getDetourBonusStat() != null ? location.getDetourBonusStat().name() : null,
                location.getDetourBonusValue()
        );
    }
}

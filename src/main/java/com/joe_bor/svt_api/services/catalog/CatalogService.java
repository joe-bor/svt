package com.joe_bor.svt_api.services.catalog;

import com.joe_bor.svt_api.controllers.catalog.dto.EventChoiceDto;
import com.joe_bor.svt_api.controllers.catalog.dto.EventDto;
import com.joe_bor.svt_api.controllers.catalog.dto.LocationDto;
import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.location.LocationEntity;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;

    public CatalogService(LocationRepository locationRepository, EventRepository eventRepository) {
        this.locationRepository = locationRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationDto> getLocations() {
        List<LocationDto> locations = new ArrayList<>();
        locationRepository.findByDetourFalseOrderByRouteOrderAsc()
                .stream()
                .map(this::toLocationDto)
                .forEach(locations::add);

        locationRepository.findByDetourTrueOrderByIdAsc()
                .stream()
                .map(this::toLocationDto)
                .forEach(locations::add);

        return locations;
    }

    @Transactional(readOnly = true)
    public List<EventDto> getEvents() {
        return eventRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toEventDto)
                .toList();
    }

    private LocationDto toLocationDto(LocationEntity location) {
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

    private EventDto toEventDto(EventEntity event) {
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getEventType().name(),
                event.getLocation() != null ? event.getLocation().getId() : null,
                event.isHasChoice(),
                event.getAutoCashEffect(),
                event.getAutoCustomersEffect(),
                event.getAutoMoraleEffect(),
                event.getAutoCoffeeEffect(),
                event.getAutoBugsEffect(),
                event.getSpecialEffect() != null ? event.getSpecialEffect().name() : null,
                event.getChoices().stream().map(this::toEventChoiceDto).toList()
        );
    }

    private EventChoiceDto toEventChoiceDto(EventChoiceEntity choice) {
        return new EventChoiceDto(
                choice.getId(),
                choice.getLabel(),
                choice.getCashEffect(),
                choice.getCustomersEffect(),
                choice.getMoraleEffect(),
                choice.getCoffeeEffect(),
                choice.getBugsEffect(),
                choice.getSpecialEffect() != null ? choice.getSpecialEffect().name() : null
        );
    }
}

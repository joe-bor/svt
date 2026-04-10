package com.joe_bor.svt_api.services.catalog;

import com.joe_bor.svt_api.controllers.catalog.dto.EventChoiceDto;
import com.joe_bor.svt_api.controllers.catalog.dto.EventDto;
import com.joe_bor.svt_api.controllers.catalog.dto.LocationDto;
import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.services.LocationDtoMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final LocationRepository locationRepository;
    private final EventRepository eventRepository;

    public List<LocationDto> getLocations() {
        return locationRepository.findAllByOrderByDetourAscRouteOrderAscIdAsc()
                .stream()
                .map(LocationDtoMapper::toLocationDto)
                .toList();
    }

    public List<EventDto> getEvents() {
        return eventRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toEventDto)
                .toList();
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

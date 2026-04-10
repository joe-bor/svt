package com.joe_bor.svt_api.services.catalog;

import com.joe_bor.svt_api.controllers.catalog.dto.EventDto;
import com.joe_bor.svt_api.controllers.catalog.dto.LocationDto;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.services.EventDtoMapper;
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
                .map(EventDtoMapper::toEventDto)
                .toList();
    }
}

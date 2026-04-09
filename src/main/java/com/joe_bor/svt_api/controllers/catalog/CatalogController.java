package com.joe_bor.svt_api.controllers.catalog;

import com.joe_bor.svt_api.controllers.catalog.dto.EventDto;
import com.joe_bor.svt_api.controllers.catalog.dto.LocationDto;
import com.joe_bor.svt_api.services.catalog.CatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/locations")
    public List<LocationDto> getLocations() {
        return catalogService.getLocations();
    }

    @GetMapping("/events")
    public List<EventDto> getEvents() {
        return catalogService.getEvents();
    }
}

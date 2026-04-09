package com.joe_bor.svt_api.controllers.catalog.dto;

import java.util.List;

public record EventDto(
        Long id,
        String name,
        String description,
        String eventType,
        Long locationId,
        boolean hasChoice,
        int autoCashEffect,
        int autoCustomersEffect,
        int autoMoraleEffect,
        int autoCoffeeEffect,
        int autoBugsEffect,
        String specialEffect,
        List<EventChoiceDto> choices
) {
}

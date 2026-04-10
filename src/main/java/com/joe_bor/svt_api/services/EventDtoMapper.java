package com.joe_bor.svt_api.services;

import com.joe_bor.svt_api.controllers.catalog.dto.EventChoiceDto;
import com.joe_bor.svt_api.controllers.catalog.dto.EventDto;
import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;

public final class EventDtoMapper {

    private EventDtoMapper() {
    }

    public static EventDto toEventDto(EventEntity event) {
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
                event.getChoices().stream().map(EventDtoMapper::toEventChoiceDto).toList()
        );
    }

    public static EventChoiceDto toEventChoiceDto(EventChoiceEntity choice) {
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

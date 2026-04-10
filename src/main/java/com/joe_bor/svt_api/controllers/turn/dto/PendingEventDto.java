package com.joe_bor.svt_api.controllers.turn.dto;

import com.joe_bor.svt_api.controllers.catalog.dto.EventDto;

public record PendingEventDto(
        int rollOrder,
        EventDto event,
        boolean requiresChoice
) {
}

package com.joe_bor.svt_api.controllers.action.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EventChoiceSelectionDto(
        @NotNull @Positive Long eventId,
        @NotNull @Positive Long choiceId
) {
}

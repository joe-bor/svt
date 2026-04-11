package com.joe_bor.svt_api.controllers.action.dto;

import com.joe_bor.svt_api.models.gameplay.ActionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record SubmitActionRequest(
        @NotNull List<@Valid EventChoiceSelectionDto> eventChoices,
        @NotNull @Valid ActionPayload action
) {
    public record ActionPayload(
            @NotNull ActionType type,
            @Positive Long destinationLocationId,
            @Positive Integer amount
    ) {
    }
}

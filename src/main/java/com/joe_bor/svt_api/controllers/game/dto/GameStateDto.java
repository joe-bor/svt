package com.joe_bor.svt_api.controllers.game.dto;

import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.controllers.catalog.dto.LocationDto;
import com.joe_bor.svt_api.controllers.turn.dto.PendingEventDto;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GameStateDto(
        UUID id,
        GameSessionStatus status,
        GameEndReason gameEndReason,
        LocalDate gameStartDate,
        LocalDate currentGameDate,
        int currentTurn,
        LocationDto currentLocation,
        StatsDto stats,
        Integer pendingCryptoSettlement,
        boolean linkedinBonusActive,
        WeatherDto weather,
        List<PendingEventDto> pendingEvents,
        List<AvailableActionDto> availableActions,
        List<AvailableNextLocationDto> availableNextLocations,
        TurnResolutionSummaryDto lastResolution
) {
}

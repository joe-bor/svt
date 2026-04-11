package com.joe_bor.svt_api.services.turn;

import com.joe_bor.svt_api.common.GameConflictException;
import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.services.game.GameSessionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves the game forward when needed and returns the current turn state.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurnService {

    private final GameSessionService gameSessionService;
    private final PendingEventService pendingEventService;

    @Transactional
    public GameStateDto advanceTurn(UUID gameId) {
        GameSessionEntity session = gameSessionService.getGameSession(gameId);

        if (session.getStatus() != GameSessionStatus.IN_PROGRESS) {
            throw new GameConflictException("Cannot advance a finished game");
        }

        if (!session.getPendingEventIds().isEmpty()) {
            return gameSessionService.getGame(session.getId());
        }

        session.setCurrentGameDate(session.getCurrentGameDate().plusDays(1));
        pendingEventService.rollEvents(session);

        return gameSessionService.getGame(session.getId());
    }
}

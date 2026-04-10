package com.joe_bor.svt_api.services.turn;

import com.joe_bor.svt_api.common.GameConflictException;
import com.joe_bor.svt_api.common.GameNotFoundException;
import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.repositories.session.GameSessionRepository;
import com.joe_bor.svt_api.services.game.GameSessionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TurnService {

    private final GameSessionRepository gameSessionRepository;
    private final GameSessionService gameSessionService;

    @Transactional
    public GameStateDto advanceTurn(UUID gameId) {
        GameSessionEntity session = gameSessionRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (session.getStatus() != GameSessionStatus.IN_PROGRESS) {
            throw new GameConflictException("Cannot advance a finished game");
        }

        return gameSessionService.getGame(session.getId());
    }
}

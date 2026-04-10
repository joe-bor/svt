package com.joe_bor.svt_api.controllers.game;

import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.services.game.GameSessionService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameSessionController {

    private final GameSessionService gameSessionService;

    @PostMapping
    public ResponseEntity<GameStateDto> createGame() {
        GameStateDto dto = gameSessionService.createGame();
        URI location = URI.create("/api/games/" + dto.id());
        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping("/{id}")
    public GameStateDto getGame(@PathVariable UUID id) {
        return gameSessionService.getGame(id);
    }
}

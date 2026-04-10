package com.joe_bor.svt_api.controllers.turn;

import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.services.turn.TurnService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{id}/turns")
@RequiredArgsConstructor
public class TurnController {

    private final TurnService turnService;

    @PostMapping("/next")
    public GameStateDto advanceTurn(@PathVariable UUID id) {
        return turnService.advanceTurn(id);
    }
}

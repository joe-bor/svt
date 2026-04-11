package com.joe_bor.svt_api.controllers.action;

import com.joe_bor.svt_api.controllers.action.dto.SubmitActionRequest;
import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.services.action.ActionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{id}/actions")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService actionService;

    @PostMapping
    public GameStateDto submitAction(@PathVariable UUID id, @Valid @RequestBody SubmitActionRequest request) {
        return actionService.resolveAction(id, request);
    }
}

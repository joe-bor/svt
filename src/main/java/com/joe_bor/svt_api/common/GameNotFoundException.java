package com.joe_bor.svt_api.common;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException(UUID id) {
        super("Game not found: " + id);
    }
}

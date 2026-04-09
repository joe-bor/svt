package com.joe_bor.svt_api.controllers;

import java.time.Instant;
import java.util.List;

public record TestResponse(
        String status,
        String environment,
        Instant timestamp,
        List<String> activeProfiles
) {
}

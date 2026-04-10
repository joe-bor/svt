package com.joe_bor.svt_api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "game.balance")
public record GameBalanceProperties(
        @Valid @NotNull StartingStats startingStats,
        @Min(1) long startingLocationId,
        @Min(1) int startDateWindowDays,
        @NotNull ZoneId timezone
) {

    public record StartingStats(
            int cash,
            @Min(0) int customers,
            @Min(0) @Max(100) int morale,
            @Min(0) int coffee,
            @Min(0) int bugs
    ) {
    }
}

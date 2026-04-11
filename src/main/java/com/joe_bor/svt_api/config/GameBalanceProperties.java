package com.joe_bor.svt_api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
        @NotNull ZoneId timezone,
        @Valid @NotNull Economy economy,
        @Valid @NotNull ActionCosts actionCosts,
        @Valid @NotNull Crypto crypto,
        @Valid @NotNull Thresholds thresholds
) {

    public record StartingStats(
            int cash,
            @Min(0) int customers,
            @Min(0) @Max(100) int morale,
            @Min(0) int coffee,
            @Min(0) int bugs
    ) {
    }

    public record Economy(
            int revenuePerCustomer,
            int operatingCost,
            @Min(0) int coffeeDecayPerTurn
    ) {
    }

    public record ActionCosts(
            @Valid @NotNull Travel travel,
            @Valid @NotNull Rest rest,
            @Valid @NotNull WorkOnProduct workOnProduct,
            @Valid @NotNull Marketing marketing,
            @Valid @NotNull PitchVcs pitchVcs,
            @Valid @NotNull BuySupplies buySupplies
    ) {
    }

    public record Travel(
            @Min(0) int cashCost,
            @Min(0) int coffeeCost
    ) {
    }

    public record Rest(
            int moraleGain,
            int coffeeGain
    ) {
    }

    public record WorkOnProduct(
            @Min(0) int moraleCost,
            @Min(0) int coffeeCost,
            @Min(0) int bugsReduced
    ) {
    }

    public record Marketing(
            @Min(0) int cashCost,
            @Min(0) int customersGained
    ) {
    }

    public record PitchVcs(
            @Min(0) int moraleCost,
            @Min(0) int coffeeCost,
            @Min(0) @Max(100) int moraleGate,
            @Min(0) int cashGained,
            @Min(0) int customersGained,
            @Min(0) int linkedinBonusCash
    ) {
    }

    public record BuySupplies(
            @Min(0) int cashCost,
            @Min(0) int coffeeGained
    ) {
    }

    public record Crypto(
            @Min(0) int minInvest,
            @Min(1) int leverageMultiplier,
            @DecimalMin("-1.0") @DecimalMax("1.0") double fallbackDeltaMin,
            @DecimalMin("-1.0") @DecimalMax("1.0") double fallbackDeltaMax
    ) {
    }

    public record Thresholds(
            int cashBankrupt,
            @Min(1) long terminalLocationId
    ) {
    }
}

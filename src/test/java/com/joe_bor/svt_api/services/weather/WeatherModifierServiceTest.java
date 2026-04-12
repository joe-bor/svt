package com.joe_bor.svt_api.services.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.controllers.game.dto.AvailableActionDto;
import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import org.junit.jupiter.api.Test;

class WeatherModifierServiceTest {

    private final WeatherModifierService service = new WeatherModifierService();

    @Test
    void computeTravelSurchargeMatchesEachBucket() {
        assertThat(service.computeTravelSurcharge(WeatherBucket.CLEAR))
                .isEqualTo(new AvailableActionDto.WeatherSurcharge(0, 0, 0));
        assertThat(service.computeTravelSurcharge(WeatherBucket.FOGGY))
                .isEqualTo(new AvailableActionDto.WeatherSurcharge(1, 0, 0));
        assertThat(service.computeTravelSurcharge(WeatherBucket.RAINY))
                .isEqualTo(new AvailableActionDto.WeatherSurcharge(1, 0, 0));
        assertThat(service.computeTravelSurcharge(WeatherBucket.STORMY))
                .isEqualTo(new AvailableActionDto.WeatherSurcharge(2, 0, 0));
    }

    @Test
    void computeTemperatureModifierMatchesEachBracket() {
        assertThat(service.computeTemperatureModifier(TemperatureBracket.COLD))
                .isEqualTo(new TurnResolutionSummaryDto.TemperatureModifier(-3, 0));
        assertThat(service.computeTemperatureModifier(TemperatureBracket.NORMAL))
                .isEqualTo(new TurnResolutionSummaryDto.TemperatureModifier(0, 0));
        assertThat(service.computeTemperatureModifier(TemperatureBracket.HOT))
                .isEqualTo(new TurnResolutionSummaryDto.TemperatureModifier(-3, -1));
    }

    @Test
    void rainySkipsCoffeeDecayOnlyForRainy() {
        assertThat(service.shouldSkipCoffeeDecay(WeatherBucket.CLEAR)).isFalse();
        assertThat(service.shouldSkipCoffeeDecay(WeatherBucket.FOGGY)).isFalse();
        assertThat(service.shouldSkipCoffeeDecay(WeatherBucket.RAINY)).isTrue();
        assertThat(service.shouldSkipCoffeeDecay(WeatherBucket.STORMY)).isFalse();
    }

    @Test
    void computeStormyTravelPenaltyReturnsLockedDeltas() {
        assertThat(service.computeStormyTravelPenalty())
                .isEqualTo(new WeatherModifierService.StormyTravelPenalty(500, -5));
    }
}

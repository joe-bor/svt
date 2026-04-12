package com.joe_bor.svt_api.services.weather;

import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.controllers.game.dto.AvailableActionDto;
import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import org.springframework.stereotype.Service;

@Service
public class WeatherModifierService {

    private static final AvailableActionDto.WeatherSurcharge ZERO_TRAVEL_SURCHARGE =
            new AvailableActionDto.WeatherSurcharge(0, 0, 0);
    private static final TurnResolutionSummaryDto.TemperatureModifier ZERO_TEMPERATURE_MODIFIER =
            new TurnResolutionSummaryDto.TemperatureModifier(0, 0);
    private static final StormyTravelPenalty ZERO_STORMY_TRAVEL_PENALTY = new StormyTravelPenalty(0, 0);
    private static final StormyTravelPenalty STORMY_TRAVEL_PENALTY = new StormyTravelPenalty(500, -5);

    public AvailableActionDto.WeatherSurcharge computeTravelSurcharge(WeatherBucket bucket) {
        return switch (bucket) {
            case CLEAR -> ZERO_TRAVEL_SURCHARGE;
            case FOGGY, RAINY -> new AvailableActionDto.WeatherSurcharge(1, 0, 0);
            case STORMY -> new AvailableActionDto.WeatherSurcharge(2, 0, 0);
        };
    }

    public TurnResolutionSummaryDto.TemperatureModifier computeTemperatureModifier(TemperatureBracket bracket) {
        return switch (bracket) {
            case COLD -> new TurnResolutionSummaryDto.TemperatureModifier(-3, 0);
            case NORMAL -> ZERO_TEMPERATURE_MODIFIER;
            case HOT -> new TurnResolutionSummaryDto.TemperatureModifier(-3, -1);
        };
    }

    public boolean shouldSkipCoffeeDecay(WeatherBucket bucket) {
        return bucket == WeatherBucket.RAINY;
    }

    public StormyTravelPenalty computeStormyTravelPenalty() {
        return STORMY_TRAVEL_PENALTY;
    }

    public StormyTravelPenalty computeStormyTravelPenalty(WeatherBucket bucket) {
        return bucket == WeatherBucket.STORMY ? STORMY_TRAVEL_PENALTY : ZERO_STORMY_TRAVEL_PENALTY;
    }

    public record StormyTravelPenalty(
            int cash,
            int morale
    ) {
    }
}

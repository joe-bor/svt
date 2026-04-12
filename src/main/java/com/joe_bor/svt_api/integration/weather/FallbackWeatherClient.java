package com.joe_bor.svt_api.integration.weather;

import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.services.random.RandomProvider;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FallbackWeatherClient implements WeatherClient {

    private static final double MIN_APPARENT_TEMPERATURE_F = 45.0;
    private static final double MAX_APPARENT_TEMPERATURE_F = 95.0;

    private final RandomProvider randomProvider;

    @Override
    public WeatherSnapshot fetch(double latitude, double longitude, LocalDate date, ZoneId timezone) {
        WeatherBucket bucket = switch (randomProvider.nextInt(4)) {
            case 0 -> WeatherBucket.CLEAR;
            case 1 -> WeatherBucket.FOGGY;
            case 2 -> WeatherBucket.RAINY;
            default -> WeatherBucket.STORMY;
        };

        double apparentTemperatureMaxF = randomProvider.nextDouble(
                MIN_APPARENT_TEMPERATURE_F,
                Math.nextUp(MAX_APPARENT_TEMPERATURE_F)
        );
        TemperatureBracket temperatureBracket = TemperatureBracket.fromFahrenheit(apparentTemperatureMaxF);
        int weatherCode = representativeCode(bucket);
        return new WeatherSnapshot(weatherCode, bucket, apparentTemperatureMaxF, temperatureBracket, true);
    }

    private static int representativeCode(WeatherBucket bucket) {
        return switch (bucket) {
            case CLEAR -> 0;
            case FOGGY -> 45;
            case RAINY -> 61;
            case STORMY -> 95;
        };
    }
}

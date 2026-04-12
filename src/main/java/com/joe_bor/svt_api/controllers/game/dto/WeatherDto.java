package com.joe_bor.svt_api.controllers.game.dto;

import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;

public record WeatherDto(
        Integer weatherCode,
        WeatherBucket bucket,
        Double apparentTemperatureMaxF,
        TemperatureBracket temperatureBracket,
        boolean fallback
) {

    public static WeatherDto fromSnapshot(WeatherSnapshot snapshot) {
        return new WeatherDto(
                snapshot.weatherCode(),
                snapshot.bucket(),
                snapshot.apparentTemperatureMaxF(),
                snapshot.temperatureBracket(),
                snapshot.fallback()
        );
    }
}

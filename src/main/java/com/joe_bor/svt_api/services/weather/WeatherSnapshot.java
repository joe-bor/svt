package com.joe_bor.svt_api.services.weather;

import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;

public record WeatherSnapshot(
        int weatherCode,
        WeatherBucket bucket,
        double apparentTemperatureMaxF,
        TemperatureBracket temperatureBracket,
        boolean fallback
) {
}

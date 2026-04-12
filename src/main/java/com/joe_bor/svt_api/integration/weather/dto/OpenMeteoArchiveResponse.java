package com.joe_bor.svt_api.integration.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenMeteoArchiveResponse(
        Daily daily
) {

    public record Daily(
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("apparent_temperature_max") List<Double> apparentTemperatureMax
    ) {
    }
}

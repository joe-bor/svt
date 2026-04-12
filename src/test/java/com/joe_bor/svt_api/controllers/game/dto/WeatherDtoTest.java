package com.joe_bor.svt_api.controllers.game.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
import org.junit.jupiter.api.Test;

class WeatherDtoTest {

    @Test
    void fromSnapshotMapsContractFields() {
        WeatherSnapshot snapshot = new WeatherSnapshot(61, WeatherBucket.RAINY, 72.5, TemperatureBracket.NORMAL, true);

        WeatherDto dto = WeatherDto.fromSnapshot(snapshot);

        assertThat(dto.weatherCode()).isEqualTo(61);
        assertThat(dto.bucket()).isEqualTo(WeatherBucket.RAINY);
        assertThat(dto.apparentTemperatureMaxF()).isEqualTo(72.5);
        assertThat(dto.temperatureBracket()).isEqualTo(TemperatureBracket.NORMAL);
        assertThat(dto.fallback()).isTrue();
    }
}

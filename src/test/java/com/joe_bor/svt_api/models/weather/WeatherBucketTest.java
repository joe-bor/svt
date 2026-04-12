package com.joe_bor.svt_api.models.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WeatherBucketTest {

    @Test
    void fromWmoCodeMapsLockedRanges() {
        assertThat(WeatherBucket.fromWmoCode(0)).isEqualTo(WeatherBucket.CLEAR);
        assertThat(WeatherBucket.fromWmoCode(1)).isEqualTo(WeatherBucket.CLEAR);
        assertThat(WeatherBucket.fromWmoCode(2)).isEqualTo(WeatherBucket.FOGGY);
        assertThat(WeatherBucket.fromWmoCode(45)).isEqualTo(WeatherBucket.FOGGY);
        assertThat(WeatherBucket.fromWmoCode(48)).isEqualTo(WeatherBucket.FOGGY);
        assertThat(WeatherBucket.fromWmoCode(51)).isEqualTo(WeatherBucket.RAINY);
        assertThat(WeatherBucket.fromWmoCode(67)).isEqualTo(WeatherBucket.RAINY);
        assertThat(WeatherBucket.fromWmoCode(80)).isEqualTo(WeatherBucket.RAINY);
        assertThat(WeatherBucket.fromWmoCode(82)).isEqualTo(WeatherBucket.RAINY);
        assertThat(WeatherBucket.fromWmoCode(71)).isEqualTo(WeatherBucket.STORMY);
        assertThat(WeatherBucket.fromWmoCode(77)).isEqualTo(WeatherBucket.STORMY);
        assertThat(WeatherBucket.fromWmoCode(95)).isEqualTo(WeatherBucket.STORMY);
        assertThat(WeatherBucket.fromWmoCode(99)).isEqualTo(WeatherBucket.STORMY);
    }

    @Test
    void fromWmoCodeRejectsUnsupportedCode() {
        assertThatThrownBy(() -> WeatherBucket.fromWmoCode(49))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported WMO weather code");
    }
}

package com.joe_bor.svt_api.models.weather;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemperatureBracketTest {

    @Test
    void fromFahrenheitUsesLockedBoundaries() {
        assertThat(TemperatureBracket.fromFahrenheit(49.9)).isEqualTo(TemperatureBracket.COLD);
        assertThat(TemperatureBracket.fromFahrenheit(50.0)).isEqualTo(TemperatureBracket.NORMAL);
        assertThat(TemperatureBracket.fromFahrenheit(85.0)).isEqualTo(TemperatureBracket.NORMAL);
        assertThat(TemperatureBracket.fromFahrenheit(85.1)).isEqualTo(TemperatureBracket.HOT);
    }
}

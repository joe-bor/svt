package com.joe_bor.svt_api.integration.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.services.random.RandomProvider;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class FallbackWeatherClientTest {

    @Test
    void fetchProducesDeterministicFallbackSnapshot() {
        FallbackWeatherClient client = new FallbackWeatherClient(new StubRandomProvider(2, 88.5));

        var snapshot = client.fetch(37.3382, -121.8863, LocalDate.of(2026, 3, 5), ZoneId.of("America/Los_Angeles"));

        assertThat(snapshot.weatherCode()).isEqualTo(61);
        assertThat(snapshot.bucket()).isEqualTo(WeatherBucket.RAINY);
        assertThat(snapshot.apparentTemperatureMaxF()).isEqualTo(88.5);
        assertThat(snapshot.temperatureBracket()).isEqualTo(TemperatureBracket.HOT);
        assertThat(snapshot.fallback()).isTrue();
    }

    private record StubRandomProvider(int nextIntValue, double nextDoubleValue) implements RandomProvider {

        @Override
        public int nextInt(int bound) {
            return nextIntValue;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return nextDoubleValue;
        }
    }
}

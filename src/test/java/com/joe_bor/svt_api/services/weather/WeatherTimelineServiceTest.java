package com.joe_bor.svt_api.services.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.integration.weather.WeatherClient;
import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherTimelineServiceTest {

    @Test
    void returnsRealClientSnapshotWhenPrimarySucceeds() {
        WeatherSnapshot primarySnapshot = new WeatherSnapshot(0, WeatherBucket.CLEAR, 68.0, TemperatureBracket.NORMAL, false);
        RecordingWeatherClient primary = new RecordingWeatherClient(primarySnapshot, null);
        RecordingWeatherClient fallback = new RecordingWeatherClient(
                new WeatherSnapshot(61, WeatherBucket.RAINY, 72.0, TemperatureBracket.NORMAL, true),
                null
        );
        WeatherTimelineService service = new WeatherTimelineService(primary, fallback, balanceProperties());

        WeatherSnapshot snapshot = service.getWeather(37.3382, -121.8863, LocalDate.of(2026, 3, 5));

        assertThat(snapshot).isEqualTo(primarySnapshot);
        assertThat(primary.calls()).hasSize(1);
        assertThat(fallback.calls()).isEmpty();
        assertThat(primary.calls().getFirst().timezone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
    }

    @Test
    void fallsBackWhenPrimaryThrows() {
        RecordingWeatherClient primary = new RecordingWeatherClient(null, new IllegalStateException("boom"));
        WeatherSnapshot fallbackSnapshot = new WeatherSnapshot(95, WeatherBucket.STORMY, 91.0, TemperatureBracket.HOT, true);
        RecordingWeatherClient fallback = new RecordingWeatherClient(fallbackSnapshot, null);
        WeatherTimelineService service = new WeatherTimelineService(primary, fallback, balanceProperties());

        WeatherSnapshot snapshot = service.getWeather(37.3382, -121.8863, LocalDate.of(2026, 3, 5));

        assertThat(snapshot).isEqualTo(fallbackSnapshot);
        assertThat(snapshot.fallback()).isTrue();
        assertThat(primary.calls()).hasSize(1);
        assertThat(fallback.calls()).hasSize(1);
    }

    @Test
    void cachesSameCoordinateAndDateWithinTtl() {
        WeatherSnapshot primarySnapshot = new WeatherSnapshot(45, WeatherBucket.FOGGY, 55.0, TemperatureBracket.NORMAL, false);
        RecordingWeatherClient primary = new RecordingWeatherClient(primarySnapshot, null);
        RecordingWeatherClient fallback = new RecordingWeatherClient(
                new WeatherSnapshot(61, WeatherBucket.RAINY, 72.0, TemperatureBracket.NORMAL, true),
                null
        );
        WeatherTimelineService service = new WeatherTimelineService(primary, fallback, balanceProperties());

        WeatherSnapshot first = service.getWeather(37.3382, -121.8863, LocalDate.of(2026, 3, 5));
        WeatherSnapshot second = service.getWeather(37.3382, -121.8863, LocalDate.of(2026, 3, 5));

        assertThat(first).isSameAs(second);
        assertThat(primary.calls()).hasSize(1);
        assertThat(fallback.calls()).isEmpty();
    }

    @Test
    void differentKeysBypassCache() {
        WeatherSnapshot primarySnapshot = new WeatherSnapshot(45, WeatherBucket.FOGGY, 55.0, TemperatureBracket.NORMAL, false);
        RecordingWeatherClient primary = new RecordingWeatherClient(primarySnapshot, null);
        RecordingWeatherClient fallback = new RecordingWeatherClient(
                new WeatherSnapshot(61, WeatherBucket.RAINY, 72.0, TemperatureBracket.NORMAL, true),
                null
        );
        WeatherTimelineService service = new WeatherTimelineService(primary, fallback, balanceProperties());

        service.getWeather(37.3382, -121.8863, LocalDate.of(2026, 3, 5));
        service.getWeather(37.3382, -121.8863, LocalDate.of(2026, 3, 6));
        service.getWeather(37.4419, -122.1430, LocalDate.of(2026, 3, 5));

        assertThat(primary.calls()).hasSize(3);
    }

    private static GameBalanceProperties balanceProperties() {
        return new GameBalanceProperties(
                new GameBalanceProperties.StartingStats(8000, 5, 80, 10, 0),
                1L,
                365,
                ZoneId.of("America/Los_Angeles"),
                new GameBalanceProperties.Economy(300, 2000, 1),
                new GameBalanceProperties.ActionCosts(
                        new GameBalanceProperties.Travel(300, 2),
                        new GameBalanceProperties.Rest(15, 3),
                        new GameBalanceProperties.WorkOnProduct(5, 2, 3),
                        new GameBalanceProperties.Marketing(2000, 3),
                        new GameBalanceProperties.PitchVcs(15, 3, 60, 3000, 3, 1500),
                        new GameBalanceProperties.BuySupplies(1500, 8)
                ),
                new GameBalanceProperties.Crypto(500, 5, -0.30, 0.30),
                new GameBalanceProperties.Thresholds(-5000, 10L)
        );
    }

    private static final class RecordingWeatherClient implements WeatherClient {

        private final WeatherSnapshot response;
        private final RuntimeException failure;
        private final List<Call> calls = new ArrayList<>();

        private RecordingWeatherClient(WeatherSnapshot response, RuntimeException failure) {
            this.response = response;
            this.failure = failure;
        }

        @Override
        public WeatherSnapshot fetch(double latitude, double longitude, LocalDate date, ZoneId timezone) {
            calls.add(new Call(latitude, longitude, date, timezone));
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        private List<Call> calls() {
            return calls;
        }
    }

    private record Call(double latitude, double longitude, LocalDate date, ZoneId timezone) {
    }
}

package com.joe_bor.svt_api.support;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.integration.weather.WeatherClient;
import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
import com.joe_bor.svt_api.services.weather.WeatherTimelineService;
import java.time.LocalDate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class WeatherTestConfiguration {

    @Bean
    @Primary
    StubWeatherTimelineService stubWeatherTimelineService(GameBalanceProperties balance) {
        return new StubWeatherTimelineService(balance);
    }

    public static class StubWeatherTimelineService extends WeatherTimelineService {

        private static final WeatherSnapshot DEFAULT_SNAPSHOT =
                new WeatherSnapshot(0, WeatherBucket.CLEAR, 72.0, TemperatureBracket.NORMAL, false);

        private WeatherSnapshot snapshot = DEFAULT_SNAPSHOT;

        public StubWeatherTimelineService(GameBalanceProperties balance) {
            super(noopClient(), noopClient(), balance);
        }

        public void setSnapshot(WeatherSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        public void reset() {
            this.snapshot = DEFAULT_SNAPSHOT;
        }

        @Override
        public WeatherSnapshot getWeather(double latitude, double longitude, LocalDate date) {
            return snapshot;
        }

        private static WeatherClient noopClient() {
            return (latitude, longitude, date, timezone) -> DEFAULT_SNAPSHOT;
        }
    }
}

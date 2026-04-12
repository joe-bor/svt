package com.joe_bor.svt_api.services.weather;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.integration.weather.WeatherClient;
import java.time.Duration;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WeatherTimelineService {

    private static final Logger log = LoggerFactory.getLogger(WeatherTimelineService.class);

    private final @Qualifier("openMeteoWeatherClient") WeatherClient primaryWeatherClient;
    private final @Qualifier("fallbackWeatherClient") WeatherClient fallbackWeatherClient;
    private final GameBalanceProperties balance;
    private final Cache<WeatherLookupKey, WeatherSnapshot> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build();

    public WeatherTimelineService(
            @Qualifier("openMeteoWeatherClient") WeatherClient primaryWeatherClient,
            @Qualifier("fallbackWeatherClient") WeatherClient fallbackWeatherClient,
            GameBalanceProperties balance
    ) {
        this.primaryWeatherClient = primaryWeatherClient;
        this.fallbackWeatherClient = fallbackWeatherClient;
        this.balance = balance;
    }

    public WeatherSnapshot getWeather(double latitude, double longitude, LocalDate date) {
        WeatherLookupKey key = new WeatherLookupKey(latitude, longitude, date);
        return cache.get(key, ignored -> loadWeather(latitude, longitude, date));
    }

    private WeatherSnapshot loadWeather(double latitude, double longitude, LocalDate date) {
        try {
            return primaryWeatherClient.fetch(latitude, longitude, date, balance.timezone());
        } catch (RuntimeException ex) {
            log.warn(
                    "Falling back to synthetic weather for lat={}, lon={}, date={}: {}",
                    latitude,
                    longitude,
                    date,
                    ex.getMessage()
            );
            return fallbackWeatherClient.fetch(latitude, longitude, date, balance.timezone());
        }
    }

    private record WeatherLookupKey(
            double latitude,
            double longitude,
            LocalDate date
    ) {
    }
}

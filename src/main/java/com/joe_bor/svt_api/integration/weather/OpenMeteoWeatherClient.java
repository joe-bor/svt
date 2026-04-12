package com.joe_bor.svt_api.integration.weather;

import com.joe_bor.svt_api.config.IntegrationProperties;
import com.joe_bor.svt_api.integration.weather.dto.OpenMeteoArchiveResponse;
import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenMeteoWeatherClient implements WeatherClient {

    private final RestClient restClient;

    public OpenMeteoWeatherClient(RestClient.Builder restClientBuilder, IntegrationProperties integrationProperties) {
        this.restClient = restClientBuilder
                .baseUrl(integrationProperties.weather().baseUrl())
                .build();
    }

    @Override
    public WeatherSnapshot fetch(double latitude, double longitude, LocalDate date, ZoneId timezone) {
        OpenMeteoArchiveResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/archive")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("start_date", date)
                        .queryParam("end_date", date)
                        .queryParam("daily", "weather_code,apparent_temperature_max")
                        .queryParam("temperature_unit", "fahrenheit")
                        .queryParam("timezone", timezone.getId())
                        .build())
                .retrieve()
                .body(OpenMeteoArchiveResponse.class);

        if (response == null || response.daily() == null) {
            throw new IllegalStateException("Open-Meteo archive response missing daily payload");
        }

        int weatherCode = extractSingleValue(response.daily().weatherCode(), "weather_code");
        double apparentTemperatureMaxF =
                extractSingleValue(response.daily().apparentTemperatureMax(), "apparent_temperature_max");

        WeatherBucket bucket = WeatherBucket.fromWmoCode(weatherCode);
        TemperatureBracket temperatureBracket = TemperatureBracket.fromFahrenheit(apparentTemperatureMaxF);
        return new WeatherSnapshot(weatherCode, bucket, apparentTemperatureMaxF, temperatureBracket, false);
    }

    private static <T> T extractSingleValue(List<T> values, String fieldName) {
        if (values == null || values.size() != 1 || values.getFirst() == null) {
            throw new IllegalStateException("Open-Meteo archive response missing a single " + fieldName + " value");
        }
        return values.getFirst();
    }
}

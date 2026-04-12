package com.joe_bor.svt_api.integration.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.joe_bor.svt_api.config.IntegrationProperties;
import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

class OpenMeteoWeatherClientTest {

    @Test
    void fetchBuildsArchiveRequestWithFahrenheitAndTimezone() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenMeteoWeatherClient client = new OpenMeteoWeatherClient(builder, integrationProperties());

        server.expect(request -> {
                    assertThat(request.getMethod()).isEqualTo(GET);
                    var uri = request.getURI();
                    assertThat(uri.getScheme()).isEqualTo("https");
                    assertThat(uri.getHost()).isEqualTo("archive-api.open-meteo.com");
                    assertThat(uri.getPath()).isEqualTo("/v1/archive");

                    var query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
                    assertThat(query.getFirst("latitude")).isEqualTo("37.3382");
                    assertThat(query.getFirst("longitude")).isEqualTo("-121.8863");
                    assertThat(query.getFirst("start_date")).isEqualTo("2026-03-05");
                    assertThat(query.getFirst("end_date")).isEqualTo("2026-03-05");
                    assertThat(query.getFirst("daily")).isEqualTo("weather_code,apparent_temperature_max");
                    assertThat(query.getFirst("temperature_unit")).isEqualTo("fahrenheit");
                    assertThat(query.getFirst("timezone")).isEqualTo("America/Los_Angeles");
                })
                .andRespond(withSuccess("""
                        {
                          "daily": {
                            "weather_code": [95],
                            "apparent_temperature_max": [91.2]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var snapshot = client.fetch(37.3382, -121.8863, LocalDate.of(2026, 3, 5), ZoneId.of("America/Los_Angeles"));

        assertThat(snapshot.weatherCode()).isEqualTo(95);
        assertThat(snapshot.bucket()).isEqualTo(WeatherBucket.STORMY);
        assertThat(snapshot.apparentTemperatureMaxF()).isEqualTo(91.2);
        assertThat(snapshot.temperatureBracket()).isEqualTo(TemperatureBracket.HOT);
        assertThat(snapshot.fallback()).isFalse();
        server.verify();
    }

    @Test
    void fetchPropagatesNon2xxResponseAsFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenMeteoWeatherClient client = new OpenMeteoWeatherClient(builder, integrationProperties());

        server.expect(request -> assertThat(request.getMethod()).isEqualTo(GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.fetch(37.3382, -121.8863, LocalDate.of(2026, 3, 5), ZoneId.of("UTC")))
                .isInstanceOf(RestClientResponseException.class);
    }

    private static IntegrationProperties integrationProperties() {
        return new IntegrationProperties(new IntegrationProperties.Weather("https://archive-api.open-meteo.com"));
    }
}

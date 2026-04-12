package com.joe_bor.svt_api.integration.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.joe_bor.svt_api.config.IntegrationProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

class CoinGeckoCryptoClientTest {

    @Test
    void fetchDeltaBuildsMarketChartRangeRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CoinGeckoCryptoClient client = new CoinGeckoCryptoClient(builder, integrationProperties());
        ZoneId timezone = ZoneId.of("America/Los_Angeles");
        LocalDate startDate = LocalDate.of(2026, 3, 5);
        LocalDate endDate = startDate.plusDays(1);
        long startEpoch = startDate.atStartOfDay(timezone).toEpochSecond();
        long endEpoch = endDate.atStartOfDay(timezone).toEpochSecond();
        long startMillis = startEpoch * 1_000L;
        long endMillis = endEpoch * 1_000L;

        server.expect(request -> {
                    assertThat(request.getMethod()).isEqualTo(GET);
                    var uri = request.getURI();
                    assertThat(uri.getScheme()).isEqualTo("https");
                    assertThat(uri.getHost()).isEqualTo("api.coingecko.com");
                    assertThat(uri.getPath()).isEqualTo("/api/v3/coins/bitcoin/market_chart/range");

                    var query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
                    assertThat(query.getFirst("vs_currency")).isEqualTo("usd");
                    assertThat(query.getFirst("from")).isEqualTo(Long.toString(startEpoch));
                    assertThat(query.getFirst("to")).isEqualTo(Long.toString(endEpoch));
                })
                .andRespond(withSuccess("""
                        {
                          "prices": [
                            [%d, 50000.0],
                            [%d, 55000.0]
                          ]
                        }
                        """.formatted(startMillis, endMillis), MediaType.APPLICATION_JSON));

        double delta = client.fetchDelta(startDate, endDate, timezone);

        assertThat(delta).isEqualTo(0.10);
        server.verify();
    }

    @Test
    void fetchDeltaPropagatesNon2xxResponseAsFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CoinGeckoCryptoClient client = new CoinGeckoCryptoClient(builder, integrationProperties());

        server.expect(request -> assertThat(request.getMethod()).isEqualTo(GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.fetchDelta(
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 6),
                ZoneId.of("UTC")
        )).isInstanceOf(RestClientResponseException.class);
    }

    private static IntegrationProperties integrationProperties() {
        return new IntegrationProperties(
                new IntegrationProperties.Weather("https://archive-api.open-meteo.com"),
                new IntegrationProperties.Crypto("https://api.coingecko.com")
        );
    }
}

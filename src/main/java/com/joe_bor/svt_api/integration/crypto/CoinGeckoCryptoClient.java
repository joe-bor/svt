package com.joe_bor.svt_api.integration.crypto;

import com.joe_bor.svt_api.config.IntegrationProperties;
import com.joe_bor.svt_api.integration.crypto.dto.CoinGeckoMarketChartResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CoinGeckoCryptoClient implements CryptoClient {

    private final RestClient restClient;

    public CoinGeckoCryptoClient(RestClient.Builder restClientBuilder, IntegrationProperties integrationProperties) {
        this.restClient = restClientBuilder
                .baseUrl(integrationProperties.crypto().baseUrl())
                .build();
    }

    @Override
    public double fetchDelta(LocalDate startDate, LocalDate endDate, ZoneId timezone) {
        long startEpoch = startDate.atStartOfDay(timezone).toEpochSecond();
        long endEpoch = endDate.atStartOfDay(timezone).toEpochSecond();
        CoinGeckoMarketChartResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v3/coins/bitcoin/market_chart/range")
                        .queryParam("vs_currency", "usd")
                        .queryParam("from", startEpoch)
                        .queryParam("to", endEpoch)
                        .build())
                .retrieve()
                .body(CoinGeckoMarketChartResponse.class);

        if (response == null || response.prices() == null || response.prices().isEmpty()) {
            throw new IllegalStateException("CoinGecko market chart response missing prices payload");
        }

        BigDecimal startPrice = priceAtOrAfter(response.prices(), Instant.ofEpochSecond(startEpoch), "start");
        BigDecimal endPrice = priceAtOrAfter(response.prices(), Instant.ofEpochSecond(endEpoch), "end");
        return endPrice.subtract(startPrice).doubleValue() / startPrice.doubleValue();
    }

    private static BigDecimal priceAtOrAfter(List<List<BigDecimal>> prices, Instant target, String label) {
        return prices.stream()
                .map(CoinGeckoCryptoClient::toPricePoint)
                .filter(point -> !point.timestamp().isBefore(target))
                .map(PricePoint::priceUsd)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "CoinGecko market chart response missing %s boundary price".formatted(label)
                ));
    }

    private static PricePoint toPricePoint(List<BigDecimal> rawPoint) {
        if (rawPoint == null || rawPoint.size() < 2 || rawPoint.get(0) == null || rawPoint.get(1) == null) {
            throw new IllegalStateException("CoinGecko market chart response contains malformed price point");
        }
        return new PricePoint(
                Instant.ofEpochMilli(rawPoint.get(0).longValueExact()),
                rawPoint.get(1)
        );
    }

    private record PricePoint(Instant timestamp, BigDecimal priceUsd) {
    }
}

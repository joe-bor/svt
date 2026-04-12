package com.joe_bor.svt_api.integration.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

// CoinGecko returns prices as `[timestamp_ms, price_usd]` tuples inside the `prices` array:
// https://docs.coingecko.com/reference/coins-id-market-chart-range
public record CoinGeckoMarketChartResponse(
        @JsonProperty("prices") List<List<BigDecimal>> prices
) {
}

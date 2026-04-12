package com.joe_bor.svt_api.integration.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record CoinGeckoMarketChartResponse(
        @JsonProperty("prices") List<List<BigDecimal>> prices
) {
}

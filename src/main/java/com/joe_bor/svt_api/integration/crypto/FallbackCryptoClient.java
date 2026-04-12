package com.joe_bor.svt_api.integration.crypto;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.services.random.RandomProvider;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FallbackCryptoClient implements CryptoClient {

    private final RandomProvider randomProvider;
    private final GameBalanceProperties balance;

    @Override
    public double fetchDelta(LocalDate startDate, LocalDate endDate, ZoneId timezone) {
        return randomProvider.nextDouble(
                balance.crypto().fallbackDeltaMin(),
                Math.nextUp(balance.crypto().fallbackDeltaMax())
        );
    }
}

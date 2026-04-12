package com.joe_bor.svt_api.services.crypto;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.integration.crypto.CryptoClient;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CryptoSettlementService {

    private static final Logger log = LoggerFactory.getLogger(CryptoSettlementService.class);

    private final @Qualifier("coinGeckoCryptoClient") CryptoClient primaryCryptoClient;
    private final @Qualifier("fallbackCryptoClient") CryptoClient fallbackCryptoClient;
    private final GameBalanceProperties balance;

    public CryptoSettlementService(
            @Qualifier("coinGeckoCryptoClient") CryptoClient primaryCryptoClient,
            @Qualifier("fallbackCryptoClient") CryptoClient fallbackCryptoClient,
            GameBalanceProperties balance
    ) {
        this.primaryCryptoClient = primaryCryptoClient;
        this.fallbackCryptoClient = fallbackCryptoClient;
        this.balance = balance;
    }

    public int settle(int principal, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(1);
        ZoneId timezone = balance.timezone();
        double delta;
        try {
            delta = primaryCryptoClient.fetchDelta(startDate, endDate, timezone);
        } catch (RuntimeException ex) {
            log.warn(
                    "Falling back to synthetic crypto settlement for startDate={}, endDate={}: {}",
                    startDate,
                    endDate,
                    ex.getMessage()
            );
            delta = fallbackCryptoClient.fetchDelta(startDate, endDate, timezone);
        }

        double multiplier = 1 + (delta * balance.crypto().leverageMultiplier());
        return (int) Math.round(principal * multiplier);
    }
}

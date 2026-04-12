package com.joe_bor.svt_api.support;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.integration.crypto.CryptoClient;
import com.joe_bor.svt_api.services.crypto.CryptoSettlementService;
import java.time.LocalDate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class CryptoTestConfiguration {

    @Bean
    @Primary
    StubCryptoSettlementService stubCryptoSettlementService(GameBalanceProperties balance) {
        return new StubCryptoSettlementService(balance);
    }

    public static class StubCryptoSettlementService extends CryptoSettlementService {

        private int settlement = 1200;
        private RuntimeException failure;

        public StubCryptoSettlementService(GameBalanceProperties balance) {
            super(noopClient(), noopClient(), balance);
        }

        public void setSettlement(int settlement) {
            this.settlement = settlement;
            this.failure = null;
        }

        public void setFailure(RuntimeException failure) {
            this.failure = failure;
        }

        public void reset() {
            this.settlement = 1200;
            this.failure = null;
        }

        @Override
        public int settle(int principal, LocalDate startDate) {
            if (failure != null) {
                throw failure;
            }
            return settlement;
        }

        private static CryptoClient noopClient() {
            return (startDate, endDate, timezone) -> 0.0;
        }
    }
}

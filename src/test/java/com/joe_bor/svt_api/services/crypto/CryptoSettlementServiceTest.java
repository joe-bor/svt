package com.joe_bor.svt_api.services.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.integration.crypto.CryptoClient;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CryptoSettlementServiceTest {

    @Test
    void settleUsesPrimaryClientDeltaWhenCallSucceeds() {
        RecordingCryptoClient primary = new RecordingCryptoClient(0.10, null);
        RecordingCryptoClient fallback = new RecordingCryptoClient(-0.25, null);
        CryptoSettlementService service = new CryptoSettlementService(primary, fallback, balanceProperties());

        int settlement = service.settle(1000, LocalDate.of(2026, 3, 5));

        assertThat(settlement).isEqualTo(1500);
        assertThat(primary.calls()).hasSize(1);
        assertThat(fallback.calls()).isEmpty();
        assertThat(primary.calls().getFirst().timezone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
        assertThat(primary.calls().getFirst().endDate()).isEqualTo(LocalDate.of(2026, 3, 6));
    }

    @Test
    void settleRoundsDownToZeroWhenLeveragedLossWipesPrincipal() {
        RecordingCryptoClient primary = new RecordingCryptoClient(-0.20, null);
        RecordingCryptoClient fallback = new RecordingCryptoClient(0.05, null);
        CryptoSettlementService service = new CryptoSettlementService(primary, fallback, balanceProperties());

        int settlement = service.settle(1000, LocalDate.of(2026, 3, 5));

        assertThat(settlement).isZero();
        assertThat(primary.calls()).hasSize(1);
        assertThat(fallback.calls()).isEmpty();
    }

    @Test
    void settleFallsBackWhenPrimaryClientThrows() {
        RecordingCryptoClient primary = new RecordingCryptoClient(null, new IllegalStateException("boom"));
        RecordingCryptoClient fallback = new RecordingCryptoClient(0.08, null);
        CryptoSettlementService service = new CryptoSettlementService(primary, fallback, balanceProperties());

        int settlement = service.settle(1000, LocalDate.of(2026, 3, 5));

        assertThat(settlement).isEqualTo(1400);
        assertThat(primary.calls()).hasSize(1);
        assertThat(fallback.calls()).hasSize(1);
        assertThat(fallback.calls().getFirst().timezone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
        assertThat(fallback.calls().getFirst().endDate()).isEqualTo(LocalDate.of(2026, 3, 6));
    }

    private static GameBalanceProperties balanceProperties() {
        return new GameBalanceProperties(
                new GameBalanceProperties.StartingStats(8000, 5, 80, 10, 0),
                1L,
                365,
                ZoneId.of("America/Los_Angeles"),
                new GameBalanceProperties.Economy(300, 2000, 1),
                new GameBalanceProperties.ActionCosts(
                        new GameBalanceProperties.Travel(300, 2),
                        new GameBalanceProperties.Rest(15, 3),
                        new GameBalanceProperties.WorkOnProduct(5, 2, 3),
                        new GameBalanceProperties.Marketing(2000, 3),
                        new GameBalanceProperties.PitchVcs(15, 3, 60, 3000, 3, 1500),
                        new GameBalanceProperties.BuySupplies(1500, 8)
                ),
                new GameBalanceProperties.Crypto(500, 5, -0.30, 0.30),
                new GameBalanceProperties.Thresholds(-5000, 10L)
        );
    }

    private static final class RecordingCryptoClient implements CryptoClient {

        private final Double delta;
        private final RuntimeException failure;
        private final List<Call> calls = new ArrayList<>();

        private RecordingCryptoClient(Double delta, RuntimeException failure) {
            this.delta = delta;
            this.failure = failure;
        }

        @Override
        public double fetchDelta(LocalDate startDate, LocalDate endDate, ZoneId timezone) {
            calls.add(new Call(startDate, endDate, timezone));
            if (failure != null) {
                throw failure;
            }
            return delta;
        }

        private List<Call> calls() {
            return calls;
        }
    }

    private record Call(LocalDate startDate, LocalDate endDate, ZoneId timezone) {
    }
}

package com.joe_bor.svt_api.integration.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.services.random.RandomProvider;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class FallbackCryptoClientTest {

    @Test
    void fetchDeltaReturnsConfiguredFallbackRangeValue() {
        FallbackCryptoClient client = new FallbackCryptoClient(
                new RecordingRandomProvider(-0.12),
                balanceProperties()
        );

        double delta = client.fetchDelta(
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 6),
                ZoneId.of("America/Los_Angeles")
        );

        assertThat(delta).isEqualTo(-0.12);
        assertThat(delta).isBetween(
                balanceProperties().crypto().fallbackDeltaMin(),
                balanceProperties().crypto().fallbackDeltaMax()
        );
    }

    @Test
    void fetchDeltaDelegatesRandomnessToProviderUsingConfiguredBounds() {
        RecordingRandomProvider randomProvider = new RecordingRandomProvider(0.08);
        GameBalanceProperties balance = balanceProperties();
        FallbackCryptoClient client = new FallbackCryptoClient(randomProvider, balance);

        double delta = client.fetchDelta(
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 6),
                ZoneId.of("UTC")
        );

        assertThat(delta).isEqualTo(0.08);
        assertThat(randomProvider.lastOrigin()).isEqualTo(balance.crypto().fallbackDeltaMin());
        assertThat(randomProvider.lastBound()).isEqualTo(Math.nextUp(balance.crypto().fallbackDeltaMax()));
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

    private static final class RecordingRandomProvider implements RandomProvider {

        private final double nextDoubleValue;
        private double lastOrigin;
        private double lastBound;

        private RecordingRandomProvider(double nextDoubleValue) {
            this.nextDoubleValue = nextDoubleValue;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }

        @Override
        public double nextDouble(double origin, double bound) {
            this.lastOrigin = origin;
            this.lastBound = bound;
            return nextDoubleValue;
        }

        private double lastOrigin() {
            return lastOrigin;
        }

        private double lastBound() {
            return lastBound;
        }
    }
}

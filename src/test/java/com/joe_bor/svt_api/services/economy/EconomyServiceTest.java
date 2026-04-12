package com.joe_bor.svt_api.services.economy;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class EconomyServiceTest {

    @Test
    void applyPassiveEconomyUsesConfiguredFormula() {
        EconomyService service = new EconomyService(balanceProperties());
        GameSessionEntity session = session();
        session.setCash(8_000);
        session.setCustomers(5);

        int delta = service.applyPassiveEconomy(session);

        assertThat(delta).isEqualTo(-500);
        assertThat(session.getCash()).isEqualTo(7_500);
    }

    @Test
    void applyPassiveEconomyCanIncreaseCash() {
        EconomyService service = new EconomyService(balanceProperties());
        GameSessionEntity session = session();
        session.setCash(8_000);
        session.setCustomers(10);

        int delta = service.applyPassiveEconomy(session);

        assertThat(delta).isEqualTo(1_000);
        assertThat(session.getCash()).isEqualTo(9_000);
    }

    @Test
    void applyCryptoSettlementCreditsCashAndClearsPendingValue() {
        EconomyService service = new EconomyService(balanceProperties());
        GameSessionEntity session = session();
        session.setCash(3_000);
        session.setPendingCryptoSettlement(1_500);

        int credited = service.applyCryptoSettlement(session);

        assertThat(credited).isEqualTo(1_500);
        assertThat(session.getCash()).isEqualTo(4_500);
        assertThat(session.getPendingCryptoSettlement()).isNull();
    }

    @Test
    void applyCoffeeDecayClampsAtZero() {
        PassiveDrainService service = new PassiveDrainService(balanceProperties());
        GameSessionEntity session = session();
        session.setCoffee(0);

        int delta = service.applyCoffeeDecay(session, WeatherBucket.CLEAR);

        assertThat(delta).isZero();
        assertThat(session.getCoffee()).isZero();
    }

    @Test
    void applyCoffeeDecayReturnsActualNegativeDelta() {
        PassiveDrainService service = new PassiveDrainService(balanceProperties());
        GameSessionEntity session = session();
        session.setCoffee(4);

        int delta = service.applyCoffeeDecay(session, WeatherBucket.CLEAR);

        assertThat(delta).isEqualTo(-1);
        assertThat(session.getCoffee()).isEqualTo(3);
    }

    @Test
    void applyCoffeeDecaySkipsRainyWeather() {
        PassiveDrainService service = new PassiveDrainService(balanceProperties());
        GameSessionEntity session = session();
        session.setCoffee(4);

        int delta = service.applyCoffeeDecay(session, WeatherBucket.RAINY);

        assertThat(delta).isZero();
        assertThat(session.getCoffee()).isEqualTo(4);
    }

    private static GameSessionEntity session() {
        GameSessionEntity session = GameSessionEntity.create();
        session.setCash(0);
        session.setCustomers(0);
        session.setCoffee(0);
        return session;
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
}

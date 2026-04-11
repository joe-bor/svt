package com.joe_bor.svt_api.services.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.models.location.LocationEntity;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class WinLossEvaluatorTest {

    @Test
    void reachedSanFranciscoWinsEvenWhenOtherLossConditionsExist() {
        WinLossEvaluator evaluator = new WinLossEvaluator(balanceProperties());
        GameSessionEntity session = session();
        session.setCurrentLocation(location(10L, "San Francisco"));
        session.setCash(-6_000);

        GameEndReason reason = evaluator.evaluateAndApply(session);

        assertThat(reason).isEqualTo(GameEndReason.REACHED_SF);
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.WON);
        assertThat(session.getGameEndReason()).isEqualTo(GameEndReason.REACHED_SF);
    }

    @Test
    void cashBankruptBeatsOtherLossChecks() {
        WinLossEvaluator evaluator = new WinLossEvaluator(balanceProperties());
        GameSessionEntity session = session();
        session.setCurrentLocation(location(2L, "Santa Clara"));
        session.setCash(-5_000);
        session.setCustomers(0);
        session.setMorale(0);

        GameEndReason reason = evaluator.evaluateAndApply(session);

        assertThat(reason).isEqualTo(GameEndReason.CASH_BANKRUPT);
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.LOST);
        assertThat(session.getGameEndReason()).isEqualTo(GameEndReason.CASH_BANKRUPT);
    }

    @Test
    void customersZeroBeatsMoraleZero() {
        WinLossEvaluator evaluator = new WinLossEvaluator(balanceProperties());
        GameSessionEntity session = session();
        session.setCurrentLocation(location(2L, "Santa Clara"));
        session.setCash(-4_999);
        session.setCustomers(0);
        session.setMorale(0);

        GameEndReason reason = evaluator.evaluateAndApply(session);

        assertThat(reason).isEqualTo(GameEndReason.CUSTOMERS_ZERO);
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.LOST);
        assertThat(session.getGameEndReason()).isEqualTo(GameEndReason.CUSTOMERS_ZERO);
    }

    @Test
    void healthySessionDoesNotEnd() {
        WinLossEvaluator evaluator = new WinLossEvaluator(balanceProperties());
        GameSessionEntity session = session();
        session.setCurrentLocation(location(2L, "Santa Clara"));
        session.setCash(500);
        session.setCustomers(2);
        session.setMorale(10);

        GameEndReason reason = evaluator.evaluateAndApply(session);

        assertThat(reason).isNull();
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
        assertThat(session.getGameEndReason()).isNull();
    }

    private static GameSessionEntity session() {
        GameSessionEntity session = GameSessionEntity.create();
        session.setStatus(GameSessionStatus.IN_PROGRESS);
        session.setCash(0);
        session.setCustomers(1);
        session.setMorale(1);
        return session;
    }

    private static LocationEntity location(Long id, String name) {
        LocationEntity location = BeanUtils.instantiateClass(LocationEntity.class);
        location.setId(id);
        location.setName(name);
        return location;
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

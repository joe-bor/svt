package com.joe_bor.svt_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.environment=test")
class GameBalancePropertiesBindingTest {

    @Autowired
    private GameBalanceProperties gameBalanceProperties;

    @Test
    void bindsStartingValuesFromApplicationYaml() {
        assertThat(gameBalanceProperties.startingStats().cash()).isEqualTo(8000);
        assertThat(gameBalanceProperties.startingStats().customers()).isEqualTo(5);
        assertThat(gameBalanceProperties.startingStats().morale()).isEqualTo(80);
        assertThat(gameBalanceProperties.startingStats().coffee()).isEqualTo(10);
        assertThat(gameBalanceProperties.startingStats().bugs()).isEqualTo(0);
        assertThat(gameBalanceProperties.startingLocationId()).isEqualTo(1L);
        assertThat(gameBalanceProperties.startDateWindowDays()).isEqualTo(365);
        assertThat(gameBalanceProperties.timezone()).isEqualTo(ZoneId.of("America/Los_Angeles"));
        assertThat(gameBalanceProperties.economy().revenuePerCustomer()).isEqualTo(300);
        assertThat(gameBalanceProperties.economy().operatingCost()).isEqualTo(2000);
        assertThat(gameBalanceProperties.economy().coffeeDecayPerTurn()).isEqualTo(1);
        assertThat(gameBalanceProperties.actionCosts().travel().cashCost()).isEqualTo(300);
        assertThat(gameBalanceProperties.actionCosts().travel().coffeeCost()).isEqualTo(2);
        assertThat(gameBalanceProperties.actionCosts().rest().moraleGain()).isEqualTo(15);
        assertThat(gameBalanceProperties.actionCosts().rest().coffeeGain()).isEqualTo(3);
        assertThat(gameBalanceProperties.actionCosts().workOnProduct().moraleCost()).isEqualTo(5);
        assertThat(gameBalanceProperties.actionCosts().workOnProduct().coffeeCost()).isEqualTo(2);
        assertThat(gameBalanceProperties.actionCosts().workOnProduct().bugsReduced()).isEqualTo(3);
        assertThat(gameBalanceProperties.actionCosts().marketing().cashCost()).isEqualTo(2000);
        assertThat(gameBalanceProperties.actionCosts().marketing().customersGained()).isEqualTo(3);
        assertThat(gameBalanceProperties.actionCosts().pitchVcs().moraleCost()).isEqualTo(15);
        assertThat(gameBalanceProperties.actionCosts().pitchVcs().coffeeCost()).isEqualTo(3);
        assertThat(gameBalanceProperties.actionCosts().pitchVcs().moraleGate()).isEqualTo(60);
        assertThat(gameBalanceProperties.actionCosts().pitchVcs().cashGained()).isEqualTo(3000);
        assertThat(gameBalanceProperties.actionCosts().pitchVcs().customersGained()).isEqualTo(3);
        assertThat(gameBalanceProperties.actionCosts().pitchVcs().linkedinBonusCash()).isEqualTo(1500);
        assertThat(gameBalanceProperties.actionCosts().buySupplies().cashCost()).isEqualTo(1500);
        assertThat(gameBalanceProperties.actionCosts().buySupplies().coffeeGained()).isEqualTo(8);
        assertThat(gameBalanceProperties.crypto().minInvest()).isEqualTo(500);
        assertThat(gameBalanceProperties.crypto().leverageMultiplier()).isEqualTo(5);
        assertThat(gameBalanceProperties.crypto().fallbackDeltaMin()).isEqualTo(-0.30);
        assertThat(gameBalanceProperties.crypto().fallbackDeltaMax()).isEqualTo(0.30);
        assertThat(gameBalanceProperties.thresholds().cashBankrupt()).isEqualTo(-5000);
        assertThat(gameBalanceProperties.thresholds().terminalLocationId()).isEqualTo(10L);
    }
}

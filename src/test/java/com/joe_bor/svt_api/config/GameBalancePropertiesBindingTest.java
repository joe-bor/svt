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
    }
}

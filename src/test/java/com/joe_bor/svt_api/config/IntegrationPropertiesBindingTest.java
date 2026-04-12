package com.joe_bor.svt_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.environment=test")
class IntegrationPropertiesBindingTest {

    @Autowired
    private IntegrationProperties integrationProperties;

    @Test
    void bindsWeatherAndCryptoBaseUrlsFromApplicationYaml() {
        assertThat(integrationProperties.weather().baseUrl()).isEqualTo("https://archive-api.open-meteo.com");
        assertThat(integrationProperties.crypto().baseUrl()).isEqualTo("https://api.coingecko.com");
    }
}

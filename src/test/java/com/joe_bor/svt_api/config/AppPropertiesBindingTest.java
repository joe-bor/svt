package com.joe_bor.svt_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.environment=integration-test")
class AppPropertiesBindingTest {

    @Autowired
    private AppProperties appProperties;

    @Test
    void bindsCustomProperties() {
        assertThat(appProperties.environment()).isEqualTo("integration-test");
    }
}

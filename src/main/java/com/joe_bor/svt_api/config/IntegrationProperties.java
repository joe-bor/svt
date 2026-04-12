package com.joe_bor.svt_api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "game.integration")
public record IntegrationProperties(
        @Valid @NotNull Weather weather
) {

    public record Weather(
            @NotBlank String baseUrl
    ) {
    }
}

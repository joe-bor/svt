package com.joe_bor.svt_api.controllers;

import com.joe_bor.svt_api.config.AppProperties;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final Environment environment;
    private final AppProperties appProperties;

    public TestController(Environment environment, AppProperties appProperties) {
        this.environment = environment;
        this.appProperties = appProperties;
    }

    @GetMapping
    public TestResponse test() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return new TestResponse("ok", appProperties.environment(), Instant.now(), profiles);
    }
}

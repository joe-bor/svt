package com.joe_bor.svt_api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.environment=test")
@AutoConfigureMockMvc
class TestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTestPayload() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.environment").value("test"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.activeProfiles").isArray());
    }
}

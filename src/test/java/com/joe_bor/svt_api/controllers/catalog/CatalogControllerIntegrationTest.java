package com.joe_bor.svt_api.controllers.catalog;

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
class CatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSeededLocationsInDeterministicOrder() throws Exception {
        mockMvc.perform(get("/api/catalog/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("San Jose"))
                .andExpect(jsonPath("$[0].routeOrder").value(1))
                .andExpect(jsonPath("$[0].detour").value(false))
                .andExpect(jsonPath("$[10].id").value(11))
                .andExpect(jsonPath("$[10].detour").value(true))
                .andExpect(jsonPath("$[10].branchesFromId").value(2))
                .andExpect(jsonPath("$[12].id").value(13))
                .andExpect(jsonPath("$[12].branchesFromId").value(7));
    }

    @Test
    void returnsSeededEventsWithEmbeddedChoices() throws Exception {
        mockMvc.perform(get("/api/catalog/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(18))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].eventType").value("RANDOM"))
                .andExpect(jsonPath("$[0].locationId").doesNotExist())
                .andExpect(jsonPath("$[0].choices.length()").value(2))
                .andExpect(jsonPath("$[0].choices[0].id").value(1))
                .andExpect(jsonPath("$[13].id").value(14))
                .andExpect(jsonPath("$[13].eventType").value("LOCATION"))
                .andExpect(jsonPath("$[13].locationId").value(3));
    }
}

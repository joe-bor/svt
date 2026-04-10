package com.joe_bor.svt_api.controllers.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.environment=test")
@AutoConfigureMockMvc
class GameSessionControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createGameReturns201WithStartingState() throws Exception {
        mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        matchesRegex("/api/games/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.gameEndReason").value(nullValue()))
                .andExpect(jsonPath("$.currentTurn").value(1))
                .andExpect(jsonPath("$.currentLocation.id").value(1))
                .andExpect(jsonPath("$.currentLocation.name").value("San Jose"))
                .andExpect(jsonPath("$.stats.cash").value(8000))
                .andExpect(jsonPath("$.stats.customers").value(5))
                .andExpect(jsonPath("$.stats.morale").value(80))
                .andExpect(jsonPath("$.stats.coffee").value(10))
                .andExpect(jsonPath("$.pendingCryptoSettlement").value(nullValue()))
                .andExpect(jsonPath("$.linkedinBonusActive").value(false))
                .andExpect(jsonPath("$.weather").isMap())
                .andExpect(jsonPath("$.pendingEvents.length()").value(0))
                .andExpect(jsonPath("$.availableActions.length()").value(0))
                .andExpect(jsonPath("$.availableNextLocations.length()").value(0))
                .andExpect(jsonPath("$.lastResolution").value(nullValue()));
    }

    @Test
    void createGameStartDateIsWithinConfiguredWindow() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        LocalDate gameStartDate = LocalDate.parse(json.get("gameStartDate").asText());
        LocalDate currentGameDate = LocalDate.parse(json.get("currentGameDate").asText());
        LocalDate todayLa = LocalDate.now(ZoneId.of("America/Los_Angeles"));

        assertThat(gameStartDate).isAfterOrEqualTo(todayLa.minusDays(365)).isBeforeOrEqualTo(todayLa.minusDays(1));
        assertThat(currentGameDate).isEqualTo(gameStartDate);
    }

    @Test
    void getGameReturnsPersistedState() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String id = createJson.get("id").asText();

        MvcResult getResult = mockMvc.perform(get("/api/games/" + id))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode getJson = objectMapper.readTree(getResult.getResponse().getContentAsString());

        assertThat(getJson).isEqualTo(createJson);
    }

    @Test
    void getGameReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/games/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Game not found: 00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void createGameProducesDistinctIds() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        String secondId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText();

        assertThat(firstId).isNotEqualTo(secondId);
    }
}

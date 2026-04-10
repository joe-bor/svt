package com.joe_bor.svt_api.controllers.game;

import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createGameReturns201WithStartingState() throws Exception {
        mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
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
        MvcResult result = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        LocalDate gameStartDate = LocalDate.parse(JsonPath.read(body, "$.gameStartDate"));
        LocalDate currentGameDate = LocalDate.parse(JsonPath.read(body, "$.currentGameDate"));

        LocalDate todayLa = LocalDate.now(ZoneId.of("America/Los_Angeles"));
        LocalDate earliestAllowed = todayLa.minusDays(365);
        LocalDate latestAllowed = todayLa.minusDays(1);

        org.assertj.core.api.Assertions.assertThat(gameStartDate)
                .isAfterOrEqualTo(earliestAllowed)
                .isBeforeOrEqualTo(latestAllowed);

        org.assertj.core.api.Assertions.assertThat(currentGameDate)
                .isEqualTo(gameStartDate);
    }

    @Test
    void getGameReturnsPersistedState() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        String id = JsonPath.read(createBody, "$.id");
        String startDate = JsonPath.read(createBody, "$.gameStartDate");
        String currentDate = JsonPath.read(createBody, "$.currentGameDate");
        int cash = JsonPath.read(createBody, "$.stats.cash");
        int customers = JsonPath.read(createBody, "$.stats.customers");
        int morale = JsonPath.read(createBody, "$.stats.morale");
        int coffee = JsonPath.read(createBody, "$.stats.coffee");

        mockMvc.perform(get("/api/games/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.gameStartDate").value(startDate))
                .andExpect(jsonPath("$.currentGameDate").value(currentDate))
                .andExpect(jsonPath("$.stats.cash").value(cash))
                .andExpect(jsonPath("$.stats.customers").value(customers))
                .andExpect(jsonPath("$.stats.morale").value(morale))
                .andExpect(jsonPath("$.stats.coffee").value(coffee));
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
        MvcResult first = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = JsonPath.read(first.getResponse().getContentAsString(), "$.id");
        String secondId = JsonPath.read(second.getResponse().getContentAsString(), "$.id");

        org.assertj.core.api.Assertions.assertThat(firstId).isNotEqualTo(secondId);
    }
}

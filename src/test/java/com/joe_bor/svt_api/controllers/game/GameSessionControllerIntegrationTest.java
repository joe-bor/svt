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
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.repositories.session.GameSessionRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
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

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private LocationRepository locationRepository;

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
                .andExpect(jsonPath("$.availableNextLocations.length()").value(1))
                .andExpect(jsonPath("$.availableNextLocations[0].locationId").value(2))
                .andExpect(jsonPath("$.availableNextLocations[0].name").value("Santa Clara"))
                .andExpect(jsonPath("$.availableNextLocations[0].detour").value(false))
                .andExpect(jsonPath("$.availableNextLocations[0].routeType").value("MAIN_ROUTE"))
                .andExpect(jsonPath("$.availableNextLocations[0].eta").value(9))
                .andExpect(jsonPath("$.availableNextLocations[0].detourBonusStat").value(nullValue()))
                .andExpect(jsonPath("$.availableNextLocations[0].detourBonusValue").value(nullValue()))
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
                .andExpect(jsonPath("$.availableNextLocations.length()").value(1))
                .andExpect(jsonPath("$.availableNextLocations[0].locationId").value(2))
                .andExpect(jsonPath("$.availableNextLocations[0].name").value("Santa Clara"))
                .andExpect(jsonPath("$.availableNextLocations[0].detour").value(false))
                .andExpect(jsonPath("$.availableNextLocations[0].routeType").value("MAIN_ROUTE"))
                .andExpect(jsonPath("$.availableNextLocations[0].eta").value(9))
                .andExpect(jsonPath("$.availableNextLocations[0].detourBonusStat").value(nullValue()))
                .andExpect(jsonPath("$.availableNextLocations[0].detourBonusValue").value(nullValue()))
                .andReturn();

        JsonNode getJson = objectMapper.readTree(getResult.getResponse().getContentAsString());

        assertThat(getJson.get("availableNextLocations")).isEqualTo(createJson.get("availableNextLocations"));
        assertThat(getJson).isEqualTo(createJson);
    }

    @Test
    void getGameAtBranchPointReturnsBothMainAndDetour() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID id = UUID.fromString(createJson.get("id").asText());

        var session = gameSessionRepository.findById(id).orElseThrow();
        session.setCurrentLocation(locationRepository.findById(2L).orElseThrow());
        gameSessionRepository.save(session);

        mockMvc.perform(get("/api/games/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableNextLocations.length()").value(2))
                .andExpect(jsonPath("$.availableNextLocations[0].locationId").value(3))
                .andExpect(jsonPath("$.availableNextLocations[0].name").value("Sunnyvale"))
                .andExpect(jsonPath("$.availableNextLocations[0].detour").value(false))
                .andExpect(jsonPath("$.availableNextLocations[0].routeType").value("MAIN_ROUTE"))
                .andExpect(jsonPath("$.availableNextLocations[0].eta").value(8))
                .andExpect(jsonPath("$.availableNextLocations[0].detourBonusStat").value(nullValue()))
                .andExpect(jsonPath("$.availableNextLocations[0].detourBonusValue").value(nullValue()))
                .andExpect(jsonPath("$.availableNextLocations[1].locationId").value(11))
                .andExpect(jsonPath("$.availableNextLocations[1].name").value("Cupertino"))
                .andExpect(jsonPath("$.availableNextLocations[1].detour").value(true))
                .andExpect(jsonPath("$.availableNextLocations[1].routeType").value("DETOUR"))
                .andExpect(jsonPath("$.availableNextLocations[1].eta").value(9))
                .andExpect(jsonPath("$.availableNextLocations[1].detourBonusStat").value("BUGS"))
                .andExpect(jsonPath("$.availableNextLocations[1].detourBonusValue").value(-3));
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
    void getGameReturns400ForMalformedId() throws Exception {
        mockMvc.perform(get("/api/games/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/games/not-a-uuid"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id': not-a-uuid"));
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

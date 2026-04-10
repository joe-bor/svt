package com.joe_bor.svt_api.controllers.turn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.repositories.session.GameSessionRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.environment=test")
@AutoConfigureMockMvc
@Transactional
class TurnControllerNextTurnIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Test
    void advanceTurnRollsEventAndAdvancesDate() throws Exception {
        JsonNode createdGame = createGame();
        UUID id = UUID.fromString(createdGame.get("id").asText());
        LocalDate startDate = LocalDate.parse(createdGame.get("gameStartDate").asText());

        var session = gameSessionRepository.findById(id).orElseThrow();
        session.getPendingEventIds().clear();
        gameSessionRepository.saveAndFlush(session);

        mockMvc.perform(post("/api/games/" + id + "/turns/next").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGameDate").value(startDate.plusDays(1).toString()))
                .andExpect(jsonPath("$.currentTurn").value(2))
                .andExpect(jsonPath("$.pendingEvents.length()").value(1))
                .andExpect(jsonPath("$.pendingEvents[0].event.eventType").value("RANDOM"))
                .andExpect(jsonPath("$.lastResolution").value(nullValue()));
    }

    @Test
    void advanceTurnIsIdempotentWhenPendingEventsExist() throws Exception {
        JsonNode createdGame = createGame();
        UUID id = UUID.fromString(createdGame.get("id").asText());
        String currentGameDate = createdGame.get("currentGameDate").asText();
        List<Long> pendingEventIds = pendingEventIds(createdGame);

        MvcResult result = mockMvc.perform(post("/api/games/" + id + "/turns/next").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGameDate").value(currentGameDate))
                .andExpect(jsonPath("$.currentTurn").value(1))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(pendingEventIds(response)).containsExactlyElementsOf(pendingEventIds);
    }

    @Test
    void advanceTurnRejects409WhenGameIsOver() throws Exception {
        JsonNode createdGame = createGame();
        UUID id = UUID.fromString(createdGame.get("id").asText());

        var session = gameSessionRepository.findById(id).orElseThrow();
        session.setStatus(GameSessionStatus.WON);
        session.setGameEndReason(GameEndReason.REACHED_SF);
        gameSessionRepository.saveAndFlush(session);

        mockMvc.perform(post("/api/games/" + id + "/turns/next").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot advance a finished game"));
    }

    @Test
    void pendingEventsSurviveReload() throws Exception {
        JsonNode createdGame = createGame();
        String id = createdGame.get("id").asText();

        MvcResult result = mockMvc.perform(get("/api/games/" + id))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode reloadedGame = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(reloadedGame.get("pendingEvents")).isEqualTo(createdGame.get("pendingEvents"));
    }

    @Test
    void advanceTurnReturns404ForUnknownGame() throws Exception {
        mockMvc.perform(post("/api/games/00000000-0000-0000-0000-000000000000/turns/next")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(
                        "Game not found: 00000000-0000-0000-0000-000000000000"));
    }

    private JsonNode createGame() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<Long> pendingEventIds(JsonNode gameState) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode pendingEvent : gameState.get("pendingEvents")) {
            ids.add(pendingEvent.get("event").get("id").asLong());
        }
        return ids;
    }
}

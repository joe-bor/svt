package com.joe_bor.svt_api.controllers.action;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.repositories.session.GameSessionRepository;
import com.joe_bor.svt_api.support.CryptoTestConfiguration;
import com.joe_bor.svt_api.support.WeatherTestConfiguration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.environment=test")
@AutoConfigureMockMvc
@Transactional
@Import({WeatherTestConfiguration.class, CryptoTestConfiguration.class})
class CryptoInvestActionIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private CryptoTestConfiguration.StubCryptoSettlementService cryptoSettlementService;

    @BeforeEach
    void resetCryptoSettlementService() {
        cryptoSettlementService.reset();
    }

    @Test
    void submitActionInvestCryptoBooksPendingSettlementForNextTurn() throws Exception {
        cryptoSettlementService.setSettlement(1500);
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "INVEST_CRYPTO", "amount": 1000 }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.cash").value(6500))
                .andExpect(jsonPath("$.pendingCryptoSettlement").value(1500))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("INVEST_CRYPTO"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.cashDelta").value(-1000))
                .andExpect(jsonPath("$.lastResolution.actionResolution.notes[0]", containsString("Principal withheld")));
    }

    @Test
    void submitActionLaterTurnCreditsPendingCryptoSettlement() throws Exception {
        cryptoSettlementService.setSettlement(1500);
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "INVEST_CRYPTO", "amount": 1000 }
                                }
                                """))
                .andExpect(status().isOk());

        JsonNode nextTurnState = advanceTurn(id);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restRequest(nextTurnState).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCryptoSettlement").value(nullValue()))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.cryptoSettlementCredited").value(1500));
    }

    @Test
    void submitActionLaterTurnClearsZeroDollarSettlementWithoutReportingPositiveCredit() throws Exception {
        cryptoSettlementService.setSettlement(0);
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "INVEST_CRYPTO", "amount": 1000 }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCryptoSettlement").value(0));

        JsonNode nextTurnState = advanceTurn(id);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(restRequest(nextTurnState).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCryptoSettlement").value(nullValue()))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.cryptoSettlementCredited").value(nullValue()));
    }

    private UUID createGameId() throws Exception {
        return UUID.fromString(createGame().get("id").asText());
    }

    private com.joe_bor.svt_api.models.session.GameSessionEntity configureSession(
            UUID id,
            long locationId,
            Set<Long> pendingEventIds,
            int cash,
            int customers,
            int morale,
            int coffee,
            int bugs,
            boolean linkedinBonusActive,
            Integer pendingCryptoSettlement
    ) {
        var session = gameSessionRepository.findById(id).orElseThrow();
        session.setCurrentLocation(locationRepository.findById(locationId).orElseThrow());
        session.setCash(cash);
        session.setCustomers(customers);
        session.setMorale(morale);
        session.setCoffee(coffee);
        session.setBugs(bugs);
        session.setLinkedinBonusActive(linkedinBonusActive);
        session.setPendingCryptoSettlement(pendingCryptoSettlement);
        session.getPendingEventIds().clear();
        session.getPendingEventIds().addAll(pendingEventIds);
        return gameSessionRepository.saveAndFlush(session);
    }

    private JsonNode createGame() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode advanceTurn(UUID id) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/games/{id}/turns/next", id))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode restRequest(JsonNode gameState) {
        ObjectNode request = objectMapper.createObjectNode();
        ArrayNode eventChoices = request.putArray("eventChoices");
        for (JsonNode pendingEvent : gameState.path("pendingEvents")) {
            if (!pendingEvent.path("requiresChoice").asBoolean(false)) {
                continue;
            }
            JsonNode event = pendingEvent.path("event");
            ObjectNode selection = eventChoices.addObject();
            selection.put("eventId", event.path("id").asLong());
            selection.put("choiceId", stableChoice(event).path("id").asLong());
        }
        ObjectNode action = request.putObject("action");
        action.put("type", "REST");
        return request;
    }

    private static JsonNode stableChoice(JsonNode event) {
        JsonNode fallback = event.path("choices").path(0);
        for (JsonNode choice : event.path("choices")) {
            if (!choice.path("specialEffect").isNull()) {
                continue;
            }
            // Prefer neutral choices so the follow-up turn can exercise crypto settlement without
            // accidentally triggering game-over or extra-randomness branches.
            boolean zeroDelta = choice.path("cashEffect").asInt() == 0
                    && choice.path("customersEffect").asInt() == 0
                    && choice.path("moraleEffect").asInt() == 0
                    && choice.path("coffeeEffect").asInt() == 0
                    && choice.path("bugsEffect").asInt() == 0;
            if (zeroDelta) {
                return choice;
            }
            fallback = choice;
        }
        return fallback;
    }
}

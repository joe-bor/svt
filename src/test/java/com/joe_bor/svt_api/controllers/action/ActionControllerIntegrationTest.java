package com.joe_bor.svt_api.controllers.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joe_bor.svt_api.models.weather.TemperatureBracket;
import com.joe_bor.svt_api.models.weather.WeatherBucket;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.repositories.session.GameSessionRepository;
import com.joe_bor.svt_api.services.weather.WeatherSnapshot;
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
@Import(WeatherTestConfiguration.class)
class ActionControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private WeatherTestConfiguration.StubWeatherTimelineService weatherTimelineService;

    @BeforeEach
    void resetWeather() {
        weatherTimelineService.reset();
    }

    @Test
    void submitActionResolvesPendingEventsPassivePhaseAndRestAction() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(2L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.stats.cash").value(8400))
                .andExpect(jsonPath("$.stats.customers").value(8))
                .andExpect(jsonPath("$.stats.morale").value(95))
                .andExpect(jsonPath("$.stats.coffee").value(12))
                .andExpect(jsonPath("$.pendingEvents.length()").value(0))
                .andExpect(jsonPath("$.lastResolution.eventResolutions.length()").value(1))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].eventId").value(2))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].choiceId").value(nullValue()))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.cashFromEconomy").value(400))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.coffeeDecay").value(-1))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.temperatureModifier.morale").value(0))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.temperatureModifier.coffee").value(0))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("REST"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.cashDelta").value(0))
                .andExpect(jsonPath("$.lastResolution.actionResolution.coffeeDelta").value(3))
                .andExpect(jsonPath("$.lastResolution.actionResolution.moraleDelta").value(15))
                .andExpect(jsonPath("$.lastResolution.actionResolution.weatherSurcharges.coffee").value(0))
                .andExpect(jsonPath("$.lastResolution.winLoss.ended").value(false))
                .andExpect(jsonPath("$.lastResolution.winLoss.reason").value(nullValue()));
    }

    @Test
    void submitActionRainyWeatherSkipsPassiveCoffeeDecay() throws Exception {
        weatherTimelineService.setSnapshot(weather(61, WeatherBucket.RAINY, 72.0, TemperatureBracket.NORMAL));
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.coffeeDecay").value(0));
    }

    @Test
    void submitActionColdWeatherAppliesMoralePressure() throws Exception {
        weatherTimelineService.setSnapshot(weather(45, WeatherBucket.FOGGY, 49.0, TemperatureBracket.COLD));
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.temperatureModifier.morale").value(-3))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.temperatureModifier.coffee").value(0));
    }

    @Test
    void submitActionHotWeatherAppliesMoraleAndCoffeePressure() throws Exception {
        weatherTimelineService.setSnapshot(weather(95, WeatherBucket.STORMY, 91.0, TemperatureBracket.HOT));
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.temperatureModifier.morale").value(-3))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas.temperatureModifier.coffee").value(-1));
    }

    @Test
    void submitActionTravelsAndWinsAtSanFrancisco() throws Exception {
        weatherTimelineService.setSnapshot(weather(95, WeatherBucket.STORMY, 91.0, TemperatureBracket.HOT));
        UUID id = createGameId();
        configureSession(id, 9L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "TRAVEL", "destinationLocationId": 10 }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.gameEndReason").value("REACHED_SF"))
                .andExpect(jsonPath("$.currentLocation.id").value(10))
                .andExpect(jsonPath("$.stats.cash").value(7700))
                .andExpect(jsonPath("$.stats.customers").value(5))
                .andExpect(jsonPath("$.stats.coffee").value(9))
                .andExpect(jsonPath("$.stats.morale").value(72))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("TRAVEL"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.cashDelta").value(200))
                .andExpect(jsonPath("$.lastResolution.actionResolution.coffeeDelta").value(-4))
                .andExpect(jsonPath("$.lastResolution.actionResolution.moraleDelta").value(-5))
                .andExpect(jsonPath("$.lastResolution.actionResolution.destinationLocationId").value(10))
                .andExpect(jsonPath("$.lastResolution.actionResolution.weatherSurcharges.coffee").value(2))
                .andExpect(jsonPath("$.lastResolution.winLoss.ended").value(true))
                .andExpect(jsonPath("$.lastResolution.winLoss.reason").value("REACHED_SF"));
    }

    @Test
    void submitActionWorkOnProductReducesBugsAndCostsStats() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 5, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "WORK_ON_PRODUCT" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.morale").value(75))
                .andExpect(jsonPath("$.stats.coffee").value(12))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("WORK_ON_PRODUCT"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.coffeeDelta").value(-2))
                .andExpect(jsonPath("$.lastResolution.actionResolution.moraleDelta").value(-5));
    }

    @Test
    void submitActionMarketingAllowsNegativeCash() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 1_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "MARKETING" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.cash").value(-1500))
                .andExpect(jsonPath("$.stats.customers").value(8))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("MARKETING"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.cashDelta").value(-2000))
                .andExpect(jsonPath("$.lastResolution.actionResolution.customersDelta").value(3));
    }

    @Test
    void submitActionBuySuppliesAddsCoffee() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "BUY_SUPPLIES" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.cash").value(6000))
                .andExpect(jsonPath("$.stats.coffee").value(22))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("BUY_SUPPLIES"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.cashDelta").value(-1500))
                .andExpect(jsonPath("$.lastResolution.actionResolution.coffeeDelta").value(8));
    }

    @Test
    void submitActionPitchVcsConsumesLinkedinBonus() throws Exception {
        UUID id = createGameId();
        configureSession(id, 3L, Set.of(4L), 8_000, 5, 80, 10, 0, true, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "PITCH_VCS" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedinBonusActive").value(false))
                .andExpect(jsonPath("$.stats.cash").value(12000))
                .andExpect(jsonPath("$.stats.customers").value(8))
                .andExpect(jsonPath("$.stats.morale").value(65))
                .andExpect(jsonPath("$.stats.coffee").value(11))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("PITCH_VCS"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.cashDelta").value(4500));
    }

    @Test
    void submitActionInvestCryptoStoresPendingSettlement() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "INVEST_CRYPTO", "amount": 500 }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.cash").value(7000))
                .andExpect(jsonPath("$.pendingCryptoSettlement").isNumber())
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("INVEST_CRYPTO"))
                .andExpect(jsonPath("$.lastResolution.actionResolution.cashDelta").value(-500));
    }

    @Test
    void submitActionSkipResolvesBurnoutAndCanLoseOnZeroMorale() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(12L), 8_000, 5, 5, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "SKIP" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.gameEndReason").value("MORALE_ZERO"))
                .andExpect(jsonPath("$.stats.morale").value(0))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("SKIP"));
    }

    @Test
    void submitActionBugCrisisAppliesDynamicCustomerLoss() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(13L), 8_000, 4, 80, 10, 3, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.customers").value(1))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].dynamicNote").value("Lost 3 customers, bugs reset"));
    }

    @Test
    void submitActionRejectsWhenNoPendingTurnExists() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("No pending turn; call POST /turns/next first"));
    }

    @Test
    void submitActionStopsEarlyWhenEventTriggersGameOver() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(7L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [
                                    { "eventId": 7, "choiceId": 7 }
                                  ],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOST"))
                .andExpect(jsonPath("$.gameEndReason").value("ACQUIRED"))
                .andExpect(jsonPath("$.stats.cash").value(8000))
                .andExpect(jsonPath("$.pendingEvents.length()").value(0))
                .andExpect(jsonPath("$.availableActions.length()").value(0))
                .andExpect(jsonPath("$.availableNextLocations.length()").value(0))
                .andExpect(jsonPath("$.lastResolution.eventResolutions.length()").value(1))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].eventId").value(7))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].choiceId").value(7))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].triggeredGameOver").value(true))
                .andExpect(jsonPath("$.lastResolution.passiveDeltas").value(nullValue()))
                .andExpect(jsonPath("$.lastResolution.actionResolution").value(nullValue()))
                .andExpect(jsonPath("$.lastResolution.winLoss.ended").value(true))
                .andExpect(jsonPath("$.lastResolution.winLoss.reason").value("ACQUIRED"));
    }

    @Test
    void submitActionRejectsMissingChoiceAndLeavesTurnRetryable() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(7L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event choice mismatch: expected=[7], got=[]"));

        var session = gameSessionRepository.findById(id).orElseThrow();
        assertThat(session.getPendingEventIds()).containsExactly(7L);
        assertThat(session.getCash()).isEqualTo(8_000);
    }

    @Test
    void submitActionRejectsInvalidChoiceIdAndLeavesTurnRetryable() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(7L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [
                                    { "eventId": 7, "choiceId": 999 }
                                  ],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isConflict());

        var session = gameSessionRepository.findById(id).orElseThrow();
        assertThat(session.getPendingEventIds()).containsExactly(7L);
        assertThat(session.getCash()).isEqualTo(8_000);
    }

    @Test
    void submitActionRejectsInvalidTravelDestination() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "TRAVEL", "destinationLocationId": 3 }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid travel destination from San Jose"));
    }

    @Test
    void submitActionRejectsUnknownTravelDestination() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "TRAVEL", "destinationLocationId": 999 }
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Location not found: 999"));
    }

    @Test
    void submitActionRejectsSkipWithoutBurnout() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "SKIP" }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("SKIP is only legal when a forced-rest event is pending"));
    }

    @Test
    void submitActionRejectsNonSkipWhenBurnoutIsPending() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(12L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Burnout Wave forces SKIP this turn"));
    }

    @Test
    void submitActionRejectsInvestBelowMinimum() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "INVEST_CRYPTO", "amount": 499 }
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitActionRejectsInvestAboveCash() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(4L), 400, 5, 80, 10, 0, false, null);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "INVEST_CRYPTO", "amount": 500 }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invest amount exceeds current cash"));
    }

    @Test
    void submitActionRejectsFinishedGame() throws Exception {
        UUID id = createGameId();
        var session = configureSession(id, 1L, Set.of(4L), 8_000, 5, 80, 10, 0, false, null);
        session.setStatus(com.joe_bor.svt_api.models.session.GameSessionStatus.LOST);
        gameSessionRepository.saveAndFlush(session);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot submit action on a finished game"));
    }

    @Test
    void submitActionReturns404ForUnknownGame() throws Exception {
        mockMvc.perform(post("/api/games/{id}/actions", UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isNotFound());
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

    private static WeatherSnapshot weather(
            int weatherCode,
            WeatherBucket bucket,
            double apparentTemperatureMaxF,
            TemperatureBracket temperatureBracket
    ) {
        return new WeatherSnapshot(weatherCode, bucket, apparentTemperatureMaxF, temperatureBracket, false);
    }
}

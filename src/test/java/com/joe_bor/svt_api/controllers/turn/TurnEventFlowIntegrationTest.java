package com.joe_bor.svt_api.controllers.turn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.repositories.session.GameSessionRepository;
import com.joe_bor.svt_api.services.random.RandomProvider;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.environment=test")
@AutoConfigureMockMvc
@Transactional
class TurnEventFlowIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private DeterministicRandomProvider randomProvider;

    @BeforeEach
    void resetRandom() {
        randomProvider.reset();
    }

    @Test
    void nextTurnAlwaysIncludesOneRandomEvent() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 80, 10, 0, false, null, true, true);

        JsonNode response = advanceTurn(id);

        assertThat(pendingEventIds(response)).containsExactly(1L);
        assertThat(pendingEventTypes(response)).containsExactly("RANDOM");
    }

    @Test
    void sunnyvaleLocationEventFiresOnceThenStopsOnRevisit() throws Exception {
        UUID id = createGameId();
        configureSession(id, 3L, Set.of(), Set.of(), 8_000, 5, 80, 10, 0, false, null, true, true);

        JsonNode firstVisit = advanceTurn(id);
        assertThat(pendingEventIds(firstVisit)).containsExactly(1L, 14L);

        GameSessionEntity session = gameSessionRepository.findById(id).orElseThrow();
        session.getPendingEventIds().clear();
        gameSessionRepository.saveAndFlush(session);

        JsonNode revisit = advanceTurn(id);
        assertThat(pendingEventIds(revisit)).containsExactly(1L);
        assertThat(revisit.get("pendingEvents"))
                .allSatisfy(pendingEvent -> assertThat(pendingEvent.get("event").get("id").asLong()).isNotEqualTo(14L));
    }

    @Test
    void mountainViewFiresGooglerPoachingOnFirstArrival() throws Exception {
        UUID id = createGameId();
        configureSession(id, 4L, Set.of(), Set.of(), 8_000, 5, 80, 10, 0, false, null, true, true);

        JsonNode response = advanceTurn(id);

        assertThat(pendingEventIds(response)).containsExactly(1L, 15L);
    }

    @Test
    void sanJoseAddsNoLocationEvent() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 80, 10, 0, false, null, true, true);

        JsonNode response = advanceTurn(id);

        assertThat(pendingEventTypes(response)).containsExactly("RANDOM");
    }

    @Test
    void burnoutOnlyFiresOnThresholdCrossingAndReArmsAfterRecovery() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 80, 0, 0, false, null, true, true);

        JsonNode firstDrop = advanceTurn(id);
        assertThat(pendingEventIds(firstDrop)).containsExactly(1L, 12L);

        GameSessionEntity session = gameSessionRepository.findById(id).orElseThrow();
        session.getPendingEventIds().clear();
        gameSessionRepository.saveAndFlush(session);

        JsonNode stillZero = advanceTurn(id);
        assertThat(pendingEventIds(stillZero)).containsExactly(1L);

        session = gameSessionRepository.findById(id).orElseThrow();
        session.getPendingEventIds().clear();
        session.setCoffee(2);
        gameSessionRepository.saveAndFlush(session);

        JsonNode recovered = advanceTurn(id);
        assertThat(pendingEventIds(recovered)).containsExactly(1L);

        session = gameSessionRepository.findById(id).orElseThrow();
        session.getPendingEventIds().clear();
        session.setCoffee(0);
        gameSessionRepository.saveAndFlush(session);

        JsonNode droppedAgain = advanceTurn(id);
        assertThat(pendingEventIds(droppedAgain)).containsExactly(1L, 12L);
    }

    @Test
    void mutinyFiresBelowTwentyFiveButNotAtThreshold() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 24, 10, 0, false, null, true, true);

        JsonNode belowThreshold = advanceTurn(id);
        assertThat(pendingEventIds(belowThreshold)).containsExactly(1L, 11L);

        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 25, 10, 0, false, null, true, true);

        JsonNode atThreshold = advanceTurn(id);
        assertThat(pendingEventIds(atThreshold)).containsExactly(1L);
    }

    @Test
    void onlyOneConditionalFiresWithPriorityOrdering() throws Exception {
        UUID id = createGameId();
        randomProvider.enqueueDoubles(0.4);
        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 20, 0, 5, false, null, true, true);

        JsonNode response = advanceTurn(id);

        assertThat(pendingEventIds(response)).containsExactly(1L, 13L);
        assertThat(pendingEventTypes(response)).containsExactly("RANDOM", "CONDITIONAL");
    }

    @Test
    void pendingEventsAreSortedRandomThenLocationThenConditional() throws Exception {
        UUID id = createGameId();
        configureSession(id, 3L, Set.of(), Set.of(), 8_000, 5, 80, 0, 0, false, null, true, true);

        JsonNode response = advanceTurn(id);

        assertThat(pendingEventIds(response)).containsExactly(1L, 14L, 12L);
        assertThat(pendingEventTypes(response)).containsExactly("RANDOM", "LOCATION", "CONDITIONAL");
    }

    @Test
    void acquiHireGameOverTerminatesBeforePassivesAndAction() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(7L), Set.of(), 8_000, 5, 80, 10, 0, false, null, true, true);

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
                .andExpect(jsonPath("$.lastResolution.passiveDeltas").value(nullValue()))
                .andExpect(jsonPath("$.lastResolution.actionResolution").value(nullValue()))
                .andExpect(jsonPath("$.lastResolution.winLoss.ended").value(true))
                .andExpect(jsonPath("$.lastResolution.winLoss.reason").value("ACQUIRED"));
    }

    @Test
    void burnoutAndRandomEventCoFireRequireChoiceSubmissionAndForceSkip() throws Exception {
        UUID id = createGameId();
        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 80, 0, 0, false, null, true, true);

        JsonNode turn = advanceTurn(id);
        assertThat(pendingEventIds(turn)).containsExactly(1L, 12L);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [],
                                  "action": { "type": "SKIP" }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event choice mismatch: expected=[1], got=[]"));

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [
                                    { "eventId": 1, "choiceId": 1 }
                                  ],
                                  "action": { "type": "SKIP" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastResolution.eventResolutions.length()").value(2))
                .andExpect(jsonPath("$.lastResolution.actionResolution.actionType").value("SKIP"));
    }

    @Test
    void bugCrisisResolutionPublishesLockedDynamicNote() throws Exception {
        UUID id = createGameId();
        randomProvider.enqueueDoubles(0.4);
        configureSession(id, 1L, Set.of(), Set.of(), 8_000, 5, 80, 10, 5, false, null, true, true);

        advanceTurn(id);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [
                                    { "eventId": 1, "choiceId": 1 }
                                  ],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastResolution.eventResolutions[1].eventId").value(13))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[1].dynamicNote")
                        .value("Lost 5 customers, bugs reset"));
    }

    @Test
    void random5050ResolutionPublishesLockedDynamicNote() throws Exception {
        UUID id = createGameId();
        randomProvider.enqueueBooleans(true);
        configureSession(id, 1L, Set.of(11L), Set.of(), 8_000, 5, 20, 10, 0, false, null, true, true);

        mockMvc.perform(post("/api/games/{id}/actions", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventChoices": [
                                    { "eventId": 11, "choiceId": 14 }
                                  ],
                                  "action": { "type": "REST" }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].eventId").value(11))
                .andExpect(jsonPath("$.lastResolution.eventResolutions[0].dynamicNote")
                        .value("50/50 resolved positive"));
    }

    private UUID createGameId() throws Exception {
        return UUID.fromString(createGame().get("id").asText());
    }

    private JsonNode createGame() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode advanceTurn(UUID id) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/games/{id}/turns/next", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private GameSessionEntity configureSession(
            UUID id,
            long locationId,
            Set<Long> pendingEventIds,
            Set<Long> firedLocationEventIds,
            int cash,
            int customers,
            int morale,
            int coffee,
            int bugs,
            boolean linkedinBonusActive,
            Integer pendingCryptoSettlement,
            boolean burnoutReady,
            boolean mutinyReady
    ) {
        GameSessionEntity session = gameSessionRepository.findById(id).orElseThrow();
        session.setCurrentLocation(locationRepository.findById(locationId).orElseThrow());
        session.setCash(cash);
        session.setCustomers(customers);
        session.setMorale(morale);
        session.setCoffee(coffee);
        session.setBugs(bugs);
        session.setLinkedinBonusActive(linkedinBonusActive);
        session.setPendingCryptoSettlement(pendingCryptoSettlement);
        session.setBurnoutReady(burnoutReady);
        session.setMutinyReady(mutinyReady);
        session.getPendingEventIds().clear();
        session.getPendingEventIds().addAll(pendingEventIds);
        session.getFiredLocationEventIds().clear();
        session.getFiredLocationEventIds().addAll(firedLocationEventIds);
        return gameSessionRepository.saveAndFlush(session);
    }

    private List<Long> pendingEventIds(JsonNode response) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode pendingEvent : response.get("pendingEvents")) {
            ids.add(pendingEvent.get("event").get("id").asLong());
        }
        return ids;
    }

    private List<String> pendingEventTypes(JsonNode response) {
        List<String> types = new ArrayList<>();
        for (JsonNode pendingEvent : response.get("pendingEvents")) {
            types.add(pendingEvent.get("event").get("eventType").asText());
        }
        return types;
    }

    @TestConfiguration
    static class RandomTestConfiguration {

        @Bean
        @Primary
        DeterministicRandomProvider deterministicRandomProvider() {
            return new DeterministicRandomProvider();
        }
    }

    static class DeterministicRandomProvider implements RandomProvider {

        private final Deque<Integer> ints = new ArrayDeque<>();
        private final Deque<Boolean> booleans = new ArrayDeque<>();
        private final Deque<Double> doubles = new ArrayDeque<>();

        void reset() {
            ints.clear();
            booleans.clear();
            doubles.clear();
        }

        void enqueueBooleans(boolean... values) {
            for (boolean value : values) {
                booleans.addLast(value);
            }
        }

        void enqueueDoubles(double... values) {
            for (double value : values) {
                doubles.addLast(value);
            }
        }

        @Override
        public int nextInt(int bound) {
            return ints.isEmpty() ? 0 : ints.removeFirst();
        }

        @Override
        public boolean nextBoolean() {
            return booleans.isEmpty() ? false : booleans.removeFirst();
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return doubles.isEmpty() ? 0.0 : doubles.removeFirst();
        }
    }
}

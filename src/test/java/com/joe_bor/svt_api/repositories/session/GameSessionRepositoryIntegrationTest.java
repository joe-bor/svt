package com.joe_bor.svt_api.repositories.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.environment=test")
@Transactional
class GameSessionRepositoryIntegrationTest {

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savedSessionRoundTrips() {
        GameSessionEntity session = buildSession();
        UUID id = session.getId();

        gameSessionRepository.saveAndFlush(session);
        entityManager.clear();

        GameSessionEntity reloaded = gameSessionRepository.findById(id).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(GameSessionStatus.IN_PROGRESS);
        assertThat(reloaded.getGameEndReason()).isNull();
        assertThat(reloaded.getCurrentLocation().getId()).isEqualTo(1L);
        assertThat(reloaded.getGameStartDate()).isEqualTo(LocalDate.of(2025, 8, 15));
        assertThat(reloaded.getCurrentGameDate()).isEqualTo(LocalDate.of(2025, 8, 15));
        assertThat(reloaded.getCash()).isEqualTo(8000);
        assertThat(reloaded.getCustomers()).isEqualTo(5);
        assertThat(reloaded.getMorale()).isEqualTo(80);
        assertThat(reloaded.getCoffee()).isEqualTo(10);
        assertThat(reloaded.getBugs()).isEqualTo(0);
        assertThat(reloaded.isMutinyReady()).isTrue();
        assertThat(reloaded.isBurnoutReady()).isTrue();
        assertThat(reloaded.isLinkedinBonusActive()).isFalse();
        assertThat(reloaded.getPendingCryptoSettlement()).isNull();
        assertThat(reloaded.getPendingEventIds()).isEmpty();
        assertThat(reloaded.getFiredLocationEventIds()).isEmpty();
    }

    @Test
    void elementCollectionsRoundTrip() {
        GameSessionEntity session = buildSession();
        session.setPendingEventIds(Set.of(1L, 2L, 3L));
        session.setFiredLocationEventIds(Set.of(14L));
        UUID id = session.getId();

        gameSessionRepository.saveAndFlush(session);
        entityManager.clear();

        GameSessionEntity reloaded = gameSessionRepository.findById(id).orElseThrow();

        assertThat(reloaded.getPendingEventIds()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(reloaded.getFiredLocationEventIds()).containsExactlyInAnyOrder(14L);
    }

    @Test
    void cascadeDeleteClearsJoinTables() {
        GameSessionEntity session = buildSession();
        session.setPendingEventIds(Set.of(1L, 2L));
        session.setFiredLocationEventIds(Set.of(14L));
        UUID id = session.getId();

        gameSessionRepository.saveAndFlush(session);
        entityManager.clear();

        gameSessionRepository.deleteById(id);
        gameSessionRepository.flush();

        Number pendingCount = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM game_session_pending_events WHERE game_session_id = ?1")
                .setParameter(1, id)
                .getSingleResult();

        Number firedCount = (Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM game_session_fired_location_events WHERE game_session_id = ?1")
                .setParameter(1, id)
                .getSingleResult();

        assertThat(pendingCount.longValue()).isZero();
        assertThat(firedCount.longValue()).isZero();
    }

    private GameSessionEntity buildSession() {
        GameSessionEntity session = GameSessionEntity.create();
        session.setId(UUID.randomUUID());
        session.setStatus(GameSessionStatus.IN_PROGRESS);
        session.setCurrentLocation(locationRepository.getReferenceById(1L));
        session.setGameStartDate(LocalDate.of(2025, 8, 15));
        session.setCurrentGameDate(LocalDate.of(2025, 8, 15));
        session.setCash(8000);
        session.setCustomers(5);
        session.setMorale(80);
        session.setCoffee(10);
        session.setBugs(0);
        session.setMutinyReady(true);
        session.setBurnoutReady(true);
        session.setLinkedinBonusActive(false);
        return session;
    }
}

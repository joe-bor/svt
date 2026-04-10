package com.joe_bor.svt_api.services.game;

import com.joe_bor.svt_api.common.GameNotFoundException;
import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.controllers.catalog.dto.LocationDto;
import com.joe_bor.svt_api.controllers.game.dto.GameStateDto;
import com.joe_bor.svt_api.controllers.game.dto.StatsDto;
import com.joe_bor.svt_api.controllers.game.dto.WeatherDto;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.repositories.session.GameSessionRepository;
import com.joe_bor.svt_api.services.LocationDtoMapper;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final LocationRepository locationRepository;
    private final GameBalanceProperties balance;

    @Transactional
    public GameStateDto createGame() {
        var startingLocation = locationRepository.findById(balance.startingLocationId())
                .orElseThrow(() -> new IllegalStateException(
                        "Starting location not seeded: id=" + balance.startingLocationId()));

        LocalDate today = LocalDate.now(balance.timezone());
        int offsetDays = ThreadLocalRandom.current().nextInt(1, balance.startDateWindowDays() + 1);
        LocalDate startDate = today.minusDays(offsetDays);
        GameBalanceProperties.StartingStats stats = balance.startingStats();

        GameSessionEntity session = GameSessionEntity.create();
        session.setId(UUID.randomUUID());
        session.setStatus(GameSessionStatus.IN_PROGRESS);
        session.setCurrentLocation(startingLocation);
        session.setGameStartDate(startDate);
        session.setCurrentGameDate(startDate);
        session.setCash(stats.cash());
        session.setCustomers(stats.customers());
        session.setMorale(stats.morale());
        session.setCoffee(stats.coffee());
        session.setBugs(stats.bugs());
        session.setMutinyReady(true);
        session.setBurnoutReady(true);
        session.setLinkedinBonusActive(false);

        gameSessionRepository.save(session);

        return toDto(session);
    }

    public GameStateDto getGame(UUID id) {
        GameSessionEntity session = gameSessionRepository.findById(id)
                .orElseThrow(() -> new GameNotFoundException(id));
        return toDto(session);
    }

    private GameStateDto toDto(GameSessionEntity session) {
        int currentTurn = Math.toIntExact(
                ChronoUnit.DAYS.between(session.getGameStartDate(), session.getCurrentGameDate())) + 1;

        return new GameStateDto(
                session.getId(),
                session.getStatus(),
                session.getGameEndReason(),
                session.getGameStartDate(),
                session.getCurrentGameDate(),
                currentTurn,
                LocationDtoMapper.toLocationDto(session.getCurrentLocation()),
                new StatsDto(
                        session.getCash(),
                        session.getCustomers(),
                        session.getMorale(),
                        session.getCoffee()
                ),
                session.getPendingCryptoSettlement(),
                session.isLinkedinBonusActive(),
                new WeatherDto(),
                List.of(),
                List.of(),
                List.of(),
                null
        );
    }
}

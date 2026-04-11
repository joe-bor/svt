package com.joe_bor.svt_api.services.action;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.config.GameBalanceProperties;
import com.joe_bor.svt_api.controllers.game.dto.AvailableActionDto;
import com.joe_bor.svt_api.models.gameplay.ActionType;
import com.joe_bor.svt_api.models.location.LocationEntity;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import com.joe_bor.svt_api.services.route.RouteService;
import java.lang.reflect.Proxy;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class ActionAffordabilityServiceTest {

    @Test
    void forcedSkipReturnsOnlySkipAction() {
        ActionAffordabilityService service = actionAffordabilityService(routeService(Map.of(), Map.of()));

        List<AvailableActionDto> availableActions = service.computeAvailableActions(healthySession(sanFrancisco()), true);

        assertThat(availableActions)
                .singleElement()
                .satisfies(action -> {
                    assertThat(action.type()).isEqualTo(ActionType.SKIP);
                    assertThat(action.cashCost()).isZero();
                    assertThat(action.coffeeCost()).isZero();
                    assertThat(action.moraleCost()).isZero();
                    assertThat(action.weatherSurcharge())
                            .isEqualTo(new AvailableActionDto.WeatherSurcharge(0, 0, 0));
                    assertThat(action.requiresDestination()).isFalse();
                    assertThat(action.requiresAmount()).isFalse();
                    assertThat(action.minAmount()).isNull();
                    assertThat(action.maxAmount()).isNull();
                    assertThat(action.disabledReason()).isNull();
                });
    }

    @Test
    void healthySessionShowsAllSevenNonSkipActions() {
        var sanJose = location(1L, "San Jose", (short) 1, false, null);
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null);
        ActionAffordabilityService service = actionAffordabilityService(routeService(
                Map.of((short) 2, santaClara),
                Map.of(sanJose.getId(), List.of())
        ));

        List<AvailableActionDto> availableActions = service.computeAvailableActions(healthySession(sanJose), false);

        assertThat(availableActions)
                .extracting(AvailableActionDto::type)
                .containsExactly(
                        ActionType.TRAVEL,
                        ActionType.REST,
                        ActionType.WORK_ON_PRODUCT,
                        ActionType.MARKETING,
                        ActionType.PITCH_VCS,
                        ActionType.BUY_SUPPLIES,
                        ActionType.INVEST_CRYPTO
                );

        assertThat(find(availableActions, ActionType.TRAVEL))
                .satisfies(action -> {
                    assertThat(action.cashCost()).isEqualTo(300);
                    assertThat(action.coffeeCost()).isEqualTo(2);
                    assertThat(action.moraleCost()).isZero();
                    assertThat(action.weatherSurcharge())
                            .isEqualTo(new AvailableActionDto.WeatherSurcharge(0, 0, 0));
                    assertThat(action.requiresDestination()).isTrue();
                    assertThat(action.requiresAmount()).isFalse();
                    assertThat(action.minAmount()).isNull();
                    assertThat(action.maxAmount()).isNull();
                    assertThat(action.disabledReason()).isNull();
                });

        assertThat(find(availableActions, ActionType.INVEST_CRYPTO))
                .satisfies(action -> {
                    assertThat(action.cashCost()).isZero();
                    assertThat(action.coffeeCost()).isZero();
                    assertThat(action.moraleCost()).isZero();
                    assertThat(action.weatherSurcharge())
                            .isEqualTo(new AvailableActionDto.WeatherSurcharge(0, 0, 0));
                    assertThat(action.requiresDestination()).isFalse();
                    assertThat(action.requiresAmount()).isTrue();
                    assertThat(action.minAmount()).isEqualTo(500);
                    assertThat(action.maxAmount()).isEqualTo(8000);
                    assertThat(action.disabledReason()).isNull();
                });
    }

    @Test
    void lowCoffeeHidesCoffeeGatedActions() {
        var sanJose = location(1L, "San Jose", (short) 1, false, null);
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null);
        GameSessionEntity session = healthySession(sanJose);
        session.setCoffee(1);
        ActionAffordabilityService service = actionAffordabilityService(routeService(
                Map.of((short) 2, santaClara),
                Map.of(sanJose.getId(), List.of())
        ));

        List<AvailableActionDto> availableActions = service.computeAvailableActions(session, false);

        assertThat(availableActions)
                .extracting(AvailableActionDto::type)
                .containsExactly(
                        ActionType.REST,
                        ActionType.MARKETING,
                        ActionType.BUY_SUPPLIES,
                        ActionType.INVEST_CRYPTO
                );
    }

    @Test
    void lowMoraleHidesPitchVcs() {
        var sanJose = location(1L, "San Jose", (short) 1, false, null);
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null);
        GameSessionEntity session = healthySession(sanJose);
        session.setMorale(59);
        ActionAffordabilityService service = actionAffordabilityService(routeService(
                Map.of((short) 2, santaClara),
                Map.of(sanJose.getId(), List.of())
        ));

        List<AvailableActionDto> availableActions = service.computeAvailableActions(session, false);

        assertThat(availableActions)
                .extracting(AvailableActionDto::type)
                .doesNotContain(ActionType.PITCH_VCS);
    }

    @Test
    void lowCashHidesInvestCrypto() {
        var sanJose = location(1L, "San Jose", (short) 1, false, null);
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null);
        GameSessionEntity session = healthySession(sanJose);
        session.setCash(499);
        ActionAffordabilityService service = actionAffordabilityService(routeService(
                Map.of((short) 2, santaClara),
                Map.of(sanJose.getId(), List.of())
        ));

        List<AvailableActionDto> availableActions = service.computeAvailableActions(session, false);

        assertThat(availableActions)
                .extracting(AvailableActionDto::type)
                .doesNotContain(ActionType.INVEST_CRYPTO);
    }

    @Test
    void terminalLocationHidesTravel() {
        var sanFrancisco = sanFrancisco();
        ActionAffordabilityService service = actionAffordabilityService(routeService(
                Map.of(),
                Map.of(sanFrancisco.getId(), List.of())
        ));

        List<AvailableActionDto> availableActions = service.computeAvailableActions(healthySession(sanFrancisco), false);

        assertThat(availableActions)
                .extracting(AvailableActionDto::type)
                .doesNotContain(ActionType.TRAVEL);
    }

    @Test
    void negativeCashStillAllowsMarketingAndBuySupplies() {
        var sanJose = location(1L, "San Jose", (short) 1, false, null);
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null);
        GameSessionEntity session = healthySession(sanJose);
        session.setCash(-9000);
        ActionAffordabilityService service = actionAffordabilityService(routeService(
                Map.of((short) 2, santaClara),
                Map.of(sanJose.getId(), List.of())
        ));

        List<AvailableActionDto> availableActions = service.computeAvailableActions(session, false);

        assertThat(availableActions)
                .extracting(AvailableActionDto::type)
                .contains(ActionType.MARKETING, ActionType.BUY_SUPPLIES)
                .doesNotContain(ActionType.INVEST_CRYPTO);
    }

    private static ActionAffordabilityService actionAffordabilityService(RouteService routeService) {
        return new ActionAffordabilityService(balanceProperties(), routeService);
    }

    private static AvailableActionDto find(List<AvailableActionDto> actions, ActionType type) {
        return actions.stream()
                .filter(action -> action.type() == type)
                .findFirst()
                .orElseThrow();
    }

    private static GameSessionEntity healthySession(LocationEntity currentLocation) {
        GameSessionEntity session = GameSessionEntity.create();
        session.setCurrentLocation(currentLocation);
        session.setCash(8000);
        session.setCustomers(5);
        session.setMorale(80);
        session.setCoffee(10);
        session.setBugs(0);
        return session;
    }

    private static LocationEntity sanFrancisco() {
        return location(10L, "San Francisco", (short) 10, false, null);
    }

    private static LocationEntity location(
            Long id,
            String name,
            Short routeOrder,
            boolean detour,
            LocationEntity branchesFrom
    ) {
        LocationEntity location = BeanUtils.instantiateClass(LocationEntity.class);
        location.setId(id);
        location.setName(name);
        location.setRouteOrder(routeOrder);
        location.setDetour(detour);
        location.setBranchesFrom(branchesFrom);
        return location;
    }

    private static GameBalanceProperties balanceProperties() {
        return new GameBalanceProperties(
                new GameBalanceProperties.StartingStats(8000, 5, 80, 10, 0),
                1L,
                365,
                ZoneId.of("America/Los_Angeles"),
                new GameBalanceProperties.Economy(300, 2000, 1),
                new GameBalanceProperties.ActionCosts(
                        new GameBalanceProperties.Travel(300, 2),
                        new GameBalanceProperties.Rest(15, 3),
                        new GameBalanceProperties.WorkOnProduct(5, 2, 3),
                        new GameBalanceProperties.Marketing(2000, 3),
                        new GameBalanceProperties.PitchVcs(15, 3, 60, 3000, 3, 1500),
                        new GameBalanceProperties.BuySupplies(1500, 8)
                ),
                new GameBalanceProperties.Crypto(500, 5, -0.30, 0.30),
                new GameBalanceProperties.Thresholds(-5000, 10L)
        );
    }

    @SuppressWarnings("unchecked")
    private static RouteService routeService(
            Map<Short, LocationEntity> routeOrderLookup,
            Map<Long, List<LocationEntity>> branchLookup
    ) {
        LocationRepository locationRepository = (LocationRepository) Proxy.newProxyInstance(
                ActionAffordabilityServiceTest.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByRouteOrder" -> Optional.ofNullable(routeOrderLookup.get(args[0]));
                    case "findAllByBranchesFromOrderByIdAsc" -> {
                        LocationEntity branchPoint = (LocationEntity) args[0];
                        yield branchLookup.getOrDefault(branchPoint.getId(), List.of());
                    }
                    case "toString" -> "ActionAffordabilityServiceTestLocationRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        return new RouteService(locationRepository);
    }
}

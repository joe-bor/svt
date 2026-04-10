package com.joe_bor.svt_api.services.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.controllers.game.dto.RouteType;
import com.joe_bor.svt_api.models.location.DetourBonusStat;
import com.joe_bor.svt_api.models.location.LocationEntity;
import com.joe_bor.svt_api.repositories.location.LocationRepository;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class RouteServiceTest {

    @Test
    void nonBranchLocationReturnsNextMainRouteOnly() {
        var sanJose = location(1L, "San Jose", (short) 1, false, null, null, null);
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null, null, null);
        RouteService routeService = new RouteService(locationRepository(
                Map.of((short) 2, santaClara),
                Map.of(sanJose.getId(), List.of())
        ));

        assertThat(routeService.getAvailableNextLocations(sanJose))
                .singleElement()
                .satisfies(next -> {
                    assertThat(next.locationId()).isEqualTo(2L);
                    assertThat(next.name()).isEqualTo("Santa Clara");
                    assertThat(next.detour()).isFalse();
                    assertThat(next.routeType()).isEqualTo(RouteType.MAIN_ROUTE);
                    assertThat(next.eta()).isEqualTo(9);
                    assertThat(next.detourBonusStat()).isNull();
                    assertThat(next.detourBonusValue()).isNull();
                });
    }

    @Test
    void branchPointReturnsMainRouteThenDetour() {
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null, null, null);
        var sunnyvale = location(3L, "Sunnyvale", (short) 3, false, null, null, null);
        var cupertino = location(11L, "Cupertino", null, true, santaClara, DetourBonusStat.BUGS, -3);
        RouteService routeService = new RouteService(locationRepository(
                Map.of((short) 3, sunnyvale),
                Map.of(santaClara.getId(), List.of(cupertino))
        ));

        assertThat(routeService.getAvailableNextLocations(santaClara))
                .hasSize(2)
                .satisfiesExactly(
                        mainRoute -> {
                            assertThat(mainRoute.locationId()).isEqualTo(3L);
                            assertThat(mainRoute.name()).isEqualTo("Sunnyvale");
                            assertThat(mainRoute.detour()).isFalse();
                            assertThat(mainRoute.routeType()).isEqualTo(RouteType.MAIN_ROUTE);
                            assertThat(mainRoute.eta()).isEqualTo(8);
                            assertThat(mainRoute.detourBonusStat()).isNull();
                            assertThat(mainRoute.detourBonusValue()).isNull();
                        },
                        detour -> {
                            assertThat(detour.locationId()).isEqualTo(11L);
                            assertThat(detour.name()).isEqualTo("Cupertino");
                            assertThat(detour.detour()).isTrue();
                            assertThat(detour.routeType()).isEqualTo(RouteType.DETOUR);
                            assertThat(detour.eta()).isEqualTo(9);
                            assertThat(detour.detourBonusStat()).isEqualTo("BUGS");
                            assertThat(detour.detourBonusValue()).isEqualTo(-3);
                        }
                );
    }

    @Test
    void terminalLocationReturnsEmptyList() {
        var sanFrancisco = location(10L, "San Francisco", (short) 10, false, null, null, null);
        RouteService routeService = new RouteService(locationRepository(
                Map.of(),
                Map.of(sanFrancisco.getId(), List.of())
        ));

        assertThat(routeService.getAvailableNextLocations(sanFrancisco)).isEmpty();
    }

    @Test
    void detourRejoinsNextMainRouteLocation() {
        var santaClara = location(2L, "Santa Clara", (short) 2, false, null, null, null);
        var cupertino = location(11L, "Cupertino", null, true, santaClara, DetourBonusStat.BUGS, -3);
        var sunnyvale = location(3L, "Sunnyvale", (short) 3, false, null, null, null);
        RouteService routeService = new RouteService(locationRepository(
                Map.of((short) 3, sunnyvale),
                Map.of()
        ));

        assertThat(routeService.getAvailableNextLocations(cupertino))
                .singleElement()
                .satisfies(next -> {
                    assertThat(next.locationId()).isEqualTo(3L);
                    assertThat(next.name()).isEqualTo("Sunnyvale");
                    assertThat(next.detour()).isFalse();
                    assertThat(next.routeType()).isEqualTo(RouteType.MAIN_ROUTE);
                    assertThat(next.eta()).isEqualTo(8);
                    assertThat(next.detourBonusStat()).isNull();
                    assertThat(next.detourBonusValue()).isNull();
                });
    }

    @Test
    void detourWithoutAutoBonusMapsNullBonusFields() {
        var redwoodCity = location(7L, "Redwood City", (short) 7, false, null, null, null);
        var sanMateo = location(8L, "San Mateo", (short) 8, false, null, null, null);
        var halfMoonBay = location(13L, "Half Moon Bay", null, true, redwoodCity, null, null);
        RouteService routeService = new RouteService(locationRepository(
                Map.of((short) 8, sanMateo),
                Map.of(redwoodCity.getId(), List.of(halfMoonBay))
        ));

        assertThat(routeService.getAvailableNextLocations(redwoodCity))
                .element(1)
                .satisfies(detour -> {
                    assertThat(detour.locationId()).isEqualTo(13L);
                    assertThat(detour.name()).isEqualTo("Half Moon Bay");
                    assertThat(detour.detour()).isTrue();
                    assertThat(detour.routeType()).isEqualTo(RouteType.DETOUR);
                    assertThat(detour.eta()).isEqualTo(4);
                    assertThat(detour.detourBonusStat()).isNull();
                    assertThat(detour.detourBonusValue()).isNull();
                });
    }

    private static LocationEntity location(
            Long id,
            String name,
            Short routeOrder,
            boolean detour,
            LocationEntity branchesFrom,
            DetourBonusStat detourBonusStat,
            Integer detourBonusValue
    ) {
        LocationEntity location = BeanUtils.instantiateClass(LocationEntity.class);
        location.setId(id);
        location.setName(name);
        location.setRouteOrder(routeOrder);
        location.setDetour(detour);
        location.setBranchesFrom(branchesFrom);
        location.setDetourBonusStat(detourBonusStat);
        location.setDetourBonusValue(detourBonusValue);
        return location;
    }

    @SuppressWarnings("unchecked")
    private static LocationRepository locationRepository(
            Map<Short, LocationEntity> routeOrderLookup,
            Map<Long, List<LocationEntity>> branchLookup
    ) {
        return (LocationRepository) Proxy.newProxyInstance(
                RouteServiceTest.class.getClassLoader(),
                new Class<?>[]{LocationRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByRouteOrder" -> Optional.ofNullable(routeOrderLookup.get(args[0]));
                    case "findAllByBranchesFromOrderByIdAsc" -> {
                        LocationEntity branchPoint = (LocationEntity) args[0];
                        yield branchLookup.getOrDefault(branchPoint.getId(), List.of());
                    }
                    case "toString" -> "RouteServiceTestLocationRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}

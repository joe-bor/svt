package com.joe_bor.svt_api.repositories.location;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.models.location.LocationEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.environment=test")
class LocationRepositoryIntegrationTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void seededLocationCountIsExpected() {
        assertThat(locationRepository.count()).isEqualTo(13);
    }

    @Test
    void loadsMainRouteInRouteOrder() {
        List<LocationEntity> mainRoute = locationRepository.findAllByOrderByDetourAscRouteOrderAscIdAsc()
                .stream()
                .filter(location -> !location.isDetour())
                .toList();

        assertThat(mainRoute)
                .hasSize(10)
                .extracting(LocationEntity::getId)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

        assertThat(mainRoute)
                .extracting(location -> location.getRouteOrder().intValue())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    void loadsDetoursWithExpectedBranchLinks() {
        List<LocationEntity> detours = locationRepository.findAllByOrderByDetourAscRouteOrderAscIdAsc()
                .stream()
                .filter(LocationEntity::isDetour)
                .toList();

        assertThat(detours)
                .hasSize(3)
                .extracting(LocationEntity::getId)
                .containsExactly(11L, 12L, 13L);

        assertThat(detours)
                .extracting(location -> location.getBranchesFrom().getId())
                .containsExactly(2L, 6L, 7L);

        assertThat(detours)
                .extracting(LocationEntity::getRouteOrder)
                .containsOnlyNulls();
    }
}

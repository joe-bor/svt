package com.joe_bor.svt_api.repositories.location;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.environment=test")
class LocationRepositoryRouteQueryTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void findByRouteOrderReturnsLocationForExistingOrder() {
        assertThat(locationRepository.findByRouteOrder((short) 3))
                .isPresent()
                .get()
                .extracting("id", "name")
                .containsExactly(3L, "Sunnyvale");
    }

    @Test
    void findByRouteOrderReturnsEmptyForMissingOrder() {
        assertThat(locationRepository.findByRouteOrder((short) 11)).isEmpty();
    }

    @Test
    void findAllByBranchesFromOrderByIdAscReturnsDetourForSantaClara() {
        var santaClara = locationRepository.findById(2L).orElseThrow();

        assertThat(locationRepository.findAllByBranchesFromOrderByIdAsc(santaClara))
                .extracting("id")
                .containsExactly(11L);
    }

    @Test
    void findAllByBranchesFromOrderByIdAscReturnsEmptyForLocationWithoutDetours() {
        var sunnyvale = locationRepository.findById(3L).orElseThrow();

        assertThat(locationRepository.findAllByBranchesFromOrderByIdAsc(sunnyvale)).isEmpty();
    }

    @Test
    void findAllByBranchesFromOrderByIdAscReturnsDetourForRedwoodCity() {
        var redwoodCity = locationRepository.findById(7L).orElseThrow();

        assertThat(locationRepository.findAllByBranchesFromOrderByIdAsc(redwoodCity))
                .extracting("id")
                .containsExactly(13L);
    }
}

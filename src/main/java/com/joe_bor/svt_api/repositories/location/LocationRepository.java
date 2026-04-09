package com.joe_bor.svt_api.repositories.location;

import com.joe_bor.svt_api.models.location.LocationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, Long> {

    List<LocationEntity> findByDetourFalseOrderByRouteOrderAsc();

    List<LocationEntity> findByDetourTrueOrderByIdAsc();

    List<LocationEntity> findAllByOrderByIdAsc();
}

package com.joe_bor.svt_api.repositories.location;

import com.joe_bor.svt_api.models.location.LocationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, Long> {

    Optional<LocationEntity> findByRouteOrder(Short routeOrder);

    List<LocationEntity> findAllByBranchesFromOrderByIdAsc(LocationEntity branchesFrom);

    List<LocationEntity> findAllByOrderByDetourAscRouteOrderAscIdAsc();
}

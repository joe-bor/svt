package com.joe_bor.svt_api.repositories.event;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.EventType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    // Load each event with its location and choices for the catalog response.
    @EntityGraph(attributePaths = {"location", "choices"})
    List<EventEntity> findAllByOrderByIdAsc();

    List<EventEntity> findByEventTypeOrderByIdAsc(EventType eventType);

    // Load each event with its location and choices for the pending-event response.
    @EntityGraph(attributePaths = {"location", "choices"})
    List<EventEntity> findAllByIdIn(Collection<Long> ids);
}

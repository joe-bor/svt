package com.joe_bor.svt_api.repositories.event;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.EventType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    // Preload related data used by catalog DTO mapping to avoid N+1 lazy-load queries.
    @EntityGraph(attributePaths = {"location", "choices"})
    List<EventEntity> findAllByOrderByIdAsc();

    List<EventEntity> findByEventTypeOrderByIdAsc(EventType eventType);

    // Preload related data for pending-event DTO rendering to avoid lazy-load query fanout.
    @EntityGraph(attributePaths = {"location", "choices"})
    List<EventEntity> findAllByIdIn(Collection<Long> ids);
}

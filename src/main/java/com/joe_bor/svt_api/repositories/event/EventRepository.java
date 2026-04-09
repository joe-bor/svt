package com.joe_bor.svt_api.repositories.event;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.EventType;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @EntityGraph(attributePaths = {"location", "choices"})
    List<EventEntity> findAllByOrderByIdAsc();

    List<EventEntity> findByEventTypeOrderByIdAsc(EventType eventType);
}

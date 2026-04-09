package com.joe_bor.svt_api.repositories.event;

import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventChoiceRepository extends JpaRepository<EventChoiceEntity, Long> {

    List<EventChoiceEntity> findByEventIdOrderByIdAsc(Long eventId);
}

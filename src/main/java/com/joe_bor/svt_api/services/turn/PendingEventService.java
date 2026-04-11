package com.joe_bor.svt_api.services.turn;

import com.joe_bor.svt_api.controllers.turn.dto.PendingEventDto;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.EventType;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.services.EventDtoMapper;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Picks pending events for a turn and builds the pending-event response list.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PendingEventService {

    private final EventRepository eventRepository;

    public void rollEvents(GameSessionEntity session) {
        List<EventEntity> randomEvents = eventRepository.findByEventTypeOrderByIdAsc(EventType.RANDOM);
        if (randomEvents.isEmpty()) {
            throw new IllegalStateException("No random events are available to roll");
        }

        EventEntity selectedEvent = randomEvents.get(ThreadLocalRandom.current().nextInt(randomEvents.size()));
        session.getPendingEventIds().add(selectedEvent.getId());
    }

    // Build pending events in the order the API returns them.
    public List<PendingEventDto> loadPendingEventDtos(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        Collection<Long> ids = Set.copyOf(eventIds);
        List<EventEntity> sortedEvents = eventRepository.findAllByIdIn(ids).stream()
                .sorted(Comparator.comparingInt((EventEntity event) -> rollPriority(event.getEventType()))
                        .thenComparing(EventEntity::getId))
                .toList();

        return java.util.stream.IntStream.range(0, sortedEvents.size())
                .mapToObj(index -> {
                    EventEntity event = sortedEvents.get(index);
                    return new PendingEventDto(
                            index,
                            EventDtoMapper.toEventDto(event),
                            event.isHasChoice()
                    );
                })
                .toList();
    }

    private static int rollPriority(EventType type) {
        return switch (type) {
            case RANDOM -> 0;
            case LOCATION -> 1;
            case CONDITIONAL -> 2;
        };
    }
}

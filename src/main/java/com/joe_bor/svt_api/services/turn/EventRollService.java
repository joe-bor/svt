package com.joe_bor.svt_api.services.turn;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.EventType;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.services.random.RandomProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRollService {

    private final EventRepository eventRepository;
    private final RandomProvider randomProvider;
    private final ConditionalEventEvaluator conditionalEventEvaluator;

    @Transactional
    public void rollEvents(GameSessionEntity session) {
        if (!session.getPendingEventIds().isEmpty()) {
            throw new IllegalStateException("Pending events must be empty before rolling");
        }

        // 1. Roll exactly one random event every turn.
        List<EventEntity> randomEvents = eventRepository.findByEventTypeOrderByIdAsc(EventType.RANDOM);
        if (randomEvents.isEmpty()) {
            throw new IllegalStateException("No random events are available to roll");
        }
        EventEntity randomEvent = randomEvents.get(randomProvider.nextInt(randomEvents.size()));
        session.getPendingEventIds().add(randomEvent.getId());

        // 2. Add the first-arrival location event if the current stop owns one and it has not fired before.
        eventRepository.findByEventTypeAndLocation(EventType.LOCATION, session.getCurrentLocation())
                .filter(event -> !session.getFiredLocationEventIds().contains(event.getId()))
                .ifPresent(event -> {
                    session.getPendingEventIds().add(event.getId());
                    session.getFiredLocationEventIds().add(event.getId());
                });

        // 3. Evaluate conditional follow-up events after the turn's random/location rolls are decided.
        conditionalEventEvaluator.evaluateAndRoll(session);
    }
}

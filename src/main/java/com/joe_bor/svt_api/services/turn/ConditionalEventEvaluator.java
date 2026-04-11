package com.joe_bor.svt_api.services.turn;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.services.random.RandomProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConditionalEventEvaluator {

    private static final long MUTINY_EVENT_ID = 11L;
    private static final long BURNOUT_EVENT_ID = 12L;
    private static final long BUG_CRISIS_EVENT_ID = 13L;

    private final EventRepository eventRepository;
    private final RandomProvider randomProvider;

    @Transactional
    public void evaluateAndRoll(GameSessionEntity session) {
        // 1. Re-arm threshold events only after the player has recovered back across the safe side.
        if (session.getCoffee() > 0 && !session.isBurnoutReady()) {
            session.setBurnoutReady(true);
        }
        if (session.getMorale() >= 25 && !session.isMutinyReady()) {
            session.setMutinyReady(true);
        }

        // 2. Evaluate conditional events in the contract-defined priority order.
        if (shouldFireBugCrisis(session)) {
            session.getPendingEventIds().add(requiredEvent(BUG_CRISIS_EVENT_ID).getId());
            return;
        }

        if (session.getCoffee() == 0 && session.isBurnoutReady()) {
            session.getPendingEventIds().add(requiredEvent(BURNOUT_EVENT_ID).getId());
            session.setBurnoutReady(false);
            return;
        }

        if (session.getMorale() < 25 && session.isMutinyReady()) {
            session.getPendingEventIds().add(requiredEvent(MUTINY_EVENT_ID).getId());
            session.setMutinyReady(false);
        }
    }

    private boolean shouldFireBugCrisis(GameSessionEntity session) {
        if (session.getBugs() <= 0) {
            return false;
        }

        // Each bug adds a 10% crisis chance, capped at 100% once the bug count gets high enough.
        double fireChance = Math.min(session.getBugs() * 0.10, 1.0);
        return randomProvider.nextDouble(0.0, 1.0) < fireChance;
    }

    private EventEntity requiredEvent(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Conditional event not seeded: id=" + eventId));
    }
}

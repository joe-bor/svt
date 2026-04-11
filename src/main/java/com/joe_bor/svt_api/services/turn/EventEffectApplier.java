package com.joe_bor.svt_api.services.turn;

import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// Applies event-only stat changes; passive systems still run later in ActionService unless an event ends the game.
public class EventEffectApplier {

    static final long BUG_CRISIS_EVENT_ID = 13L;

    private final SpecialEffectResolver specialEffectResolver;

    @Transactional
    public TurnResolutionSummaryDto.EventResolutionDetail applyAutoEvent(GameSessionEntity session, EventEntity event) {
        VisibleStatsSnapshot before = VisibleStatsSnapshot.capture(session);
        String dynamicNote = null;

        if (event.getId() == BUG_CRISIS_EVENT_ID) {
            int lostCustomers = Math.max(0, session.getBugs());
            session.setCustomers(session.getCustomers() - lostCustomers);
            session.setBugs(0);
            dynamicNote = "Lost " + lostCustomers + " customers, bugs reset";
        } else {
            session.setCash(session.getCash() + event.getAutoCashEffect());
            session.setCustomers(session.getCustomers() + event.getAutoCustomersEffect());
            session.setMorale(session.getMorale() + event.getAutoMoraleEffect());
            session.setCoffee(session.getCoffee() + event.getAutoCoffeeEffect());
            session.setBugs(session.getBugs() + event.getAutoBugsEffect());
        }

        clampSession(session);
        VisibleStatsSnapshot after = VisibleStatsSnapshot.capture(session);

        return new TurnResolutionSummaryDto.EventResolutionDetail(
                event.getId(),
                null,
                after.deltaFrom(before),
                false,
                dynamicNote
        );
    }

    @Transactional
    public TurnResolutionSummaryDto.EventResolutionDetail applyChoiceEvent(
            GameSessionEntity session,
            EventEntity event,
            EventChoiceEntity choice
    ) {
        VisibleStatsSnapshot before = VisibleStatsSnapshot.capture(session);

        session.setCash(session.getCash() + choice.getCashEffect());
        session.setCustomers(session.getCustomers() + choice.getCustomersEffect());
        session.setMorale(session.getMorale() + choice.getMoraleEffect());
        session.setCoffee(session.getCoffee() + choice.getCoffeeEffect());
        session.setBugs(session.getBugs() + choice.getBugsEffect());

        SpecialEffectResolver.SpecialEffectResult specialEffectResult =
                specialEffectResolver.resolve(session, event, choice);

        clampSession(session);
        VisibleStatsSnapshot after = VisibleStatsSnapshot.capture(session);

        return new TurnResolutionSummaryDto.EventResolutionDetail(
                event.getId(),
                choice.getId(),
                after.deltaFrom(before),
                specialEffectResult.triggeredGameOver(),
                specialEffectResult.dynamicNote()
        );
    }

    // Event chains can push values out of bounds, so normalize before building response deltas.
    static void clampSession(GameSessionEntity session) {
        session.setMorale(Math.max(0, Math.min(100, session.getMorale())));
        session.setCoffee(Math.max(0, session.getCoffee()));
        session.setCustomers(Math.max(0, session.getCustomers()));
        session.setBugs(Math.max(0, session.getBugs()));
    }

    record VisibleStatsSnapshot(
            int cash,
            int customers,
            int morale,
            int coffee
    ) {
        static VisibleStatsSnapshot capture(GameSessionEntity session) {
            return new VisibleStatsSnapshot(
                    session.getCash(),
                    session.getCustomers(),
                    session.getMorale(),
                    session.getCoffee()
            );
        }

        private TurnResolutionSummaryDto.StatDeltas deltaFrom(VisibleStatsSnapshot before) {
            return new TurnResolutionSummaryDto.StatDeltas(
                    cash - before.cash(),
                    customers - before.customers(),
                    morale - before.morale(),
                    coffee - before.coffee()
            );
        }
    }
}

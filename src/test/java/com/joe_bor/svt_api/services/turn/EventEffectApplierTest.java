package com.joe_bor.svt_api.services.turn;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.controllers.action.dto.TurnResolutionSummaryDto;
import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.SpecialEffectType;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.services.random.RandomProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class EventEffectApplierTest {

    @Test
    void autoEventAppliesStaticEffects() {
        EventEffectApplier applier = applier(randomProvider());
        GameSessionEntity session = session();
        EventEntity event = event(2L);
        event.setAutoCustomersEffect(3);

        TurnResolutionSummaryDto.EventResolutionDetail detail = applier.applyAutoEvent(session, event);

        assertThat(session.getCustomers()).isEqualTo(8);
        assertThat(detail.eventId()).isEqualTo(2L);
        assertThat(detail.choiceId()).isNull();
        assertThat(detail.statDeltas()).isEqualTo(new TurnResolutionSummaryDto.StatDeltas(0, 3, 0, 0));
        assertThat(detail.triggeredGameOver()).isFalse();
        assertThat(detail.dynamicNote()).isNull();
    }

    @Test
    void bugCrisisAppliesDynamicCustomerLossAndResetsBugs() {
        EventEffectApplier applier = applier(randomProvider());
        GameSessionEntity session = session();
        session.setCustomers(10);
        session.setBugs(5);

        TurnResolutionSummaryDto.EventResolutionDetail detail = applier.applyAutoEvent(session, event(13L));

        assertThat(session.getCustomers()).isEqualTo(5);
        assertThat(session.getBugs()).isZero();
        assertThat(detail.statDeltas()).isEqualTo(new TurnResolutionSummaryDto.StatDeltas(0, -5, 0, 0));
        assertThat(detail.dynamicNote()).isEqualTo("Lost 5 customers, bugs reset");
    }

    @Test
    void choiceEventAppliesBaseStatEffects() {
        EventEffectApplier applier = applier(randomProvider());
        GameSessionEntity session = session();
        EventChoiceEntity choice = choice(5L);
        choice.setCashEffect(1_500);
        choice.setCoffeeEffect(-2);

        TurnResolutionSummaryDto.EventResolutionDetail detail =
                applier.applyChoiceEvent(session, event(5L), choice);

        assertThat(session.getCash()).isEqualTo(9_500);
        assertThat(session.getCoffee()).isEqualTo(8);
        assertThat(detail.choiceId()).isEqualTo(5L);
        assertThat(detail.statDeltas()).isEqualTo(new TurnResolutionSummaryDto.StatDeltas(1_500, 0, 0, -2));
    }

    @Test
    void statsClampToValidRanges() {
        EventEffectApplier applier = applier(randomProvider());
        GameSessionEntity session = session();
        session.setCustomers(1);
        session.setMorale(95);
        session.setCoffee(1);
        session.setBugs(1);
        EventChoiceEntity choice = choice(99L);
        choice.setCustomersEffect(-10);
        choice.setMoraleEffect(10);
        choice.setCoffeeEffect(-10);
        choice.setBugsEffect(-10);

        TurnResolutionSummaryDto.EventResolutionDetail detail =
                applier.applyChoiceEvent(session, event(3L), choice);

        assertThat(session.getCustomers()).isZero();
        assertThat(session.getMorale()).isEqualTo(100);
        assertThat(session.getCoffee()).isZero();
        assertThat(session.getBugs()).isZero();
        assertThat(detail.statDeltas()).isEqualTo(new TurnResolutionSummaryDto.StatDeltas(0, -1, 5, -1));
    }

    @Test
    void beforeAfterDeltaUsesPostClampVisibleStats() {
        EventEffectApplier applier = applier(randomProvider());
        GameSessionEntity session = session();
        session.setCustomers(2);
        session.setMorale(5);
        EventChoiceEntity choice = choice(3L);
        choice.setCashEffect(400);
        choice.setCustomersEffect(-5);
        choice.setMoraleEffect(-10);
        choice.setCoffeeEffect(3);

        TurnResolutionSummaryDto.EventResolutionDetail detail =
                applier.applyChoiceEvent(session, event(1L), choice);

        assertThat(detail.statDeltas()).isEqualTo(new TurnResolutionSummaryDto.StatDeltas(400, -2, -5, 3));
    }

    private static EventEffectApplier applier(RandomProvider randomProvider) {
        return new EventEffectApplier(new SpecialEffectResolver(randomProvider));
    }

    private static GameSessionEntity session() {
        GameSessionEntity session = GameSessionEntity.create();
        session.setCash(8_000);
        session.setCustomers(5);
        session.setMorale(80);
        session.setCoffee(10);
        session.setBugs(0);
        return session;
    }

    private static EventEntity event(Long id) {
        EventEntity event = BeanUtils.instantiateClass(EventEntity.class);
        event.setId(id);
        return event;
    }

    private static EventChoiceEntity choice(Long id) {
        EventChoiceEntity choice = BeanUtils.instantiateClass(EventChoiceEntity.class);
        choice.setId(id);
        choice.setSpecialEffect((SpecialEffectType) null);
        return choice;
    }

    private static RandomProvider randomProvider() {
        return new RandomProvider() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }

            @Override
            public boolean nextBoolean() {
                return false;
            }

            @Override
            public double nextDouble(double origin, double bound) {
                return 0;
            }
        };
    }
}

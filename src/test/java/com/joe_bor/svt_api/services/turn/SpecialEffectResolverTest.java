package com.joe_bor.svt_api.services.turn;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.models.event.EventChoiceEntity;
import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.SpecialEffectType;
import com.joe_bor.svt_api.models.session.GameEndReason;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.models.session.GameSessionStatus;
import com.joe_bor.svt_api.services.random.RandomProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class SpecialEffectResolverTest {

    @Test
    void acquiHireGameOverMarksGameLost() {
        SpecialEffectResolver resolver = resolver(true);
        GameSessionEntity session = session();
        EventEntity event = event(7L);
        EventChoiceEntity choice = choice(7L, SpecialEffectType.GAME_OVER);

        SpecialEffectResolver.SpecialEffectResult result = resolver.resolve(session, event, choice);

        assertThat(result.triggeredGameOver()).isTrue();
        assertThat(result.dynamicNote()).isEqualTo("Accepted Acqui-hire Offer");
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.LOST);
        assertThat(session.getGameEndReason()).isEqualTo(GameEndReason.ACQUIRED);
    }

    @Test
    void linkedinGameOverMarksGameLost() {
        SpecialEffectResolver resolver = resolver(true);
        GameSessionEntity session = session();
        EventEntity event = event(14L);
        EventChoiceEntity choice = choice(15L, SpecialEffectType.GAME_OVER);

        SpecialEffectResolver.SpecialEffectResult result = resolver.resolve(session, event, choice);

        assertThat(result.triggeredGameOver()).isTrue();
        assertThat(result.dynamicNote()).isEqualTo("Accepted LinkedIn job");
        assertThat(session.getStatus()).isEqualTo(GameSessionStatus.LOST);
        assertThat(session.getGameEndReason()).isEqualTo(GameEndReason.TOOK_LINKEDIN_JOB);
    }

    @Test
    void mutinyRandomPositiveAddsMorale() {
        SpecialEffectResolver resolver = resolver(true);
        GameSessionEntity session = session();

        SpecialEffectResolver.SpecialEffectResult result =
                resolver.resolve(session, event(11L), choice(14L, SpecialEffectType.RANDOM_5050));

        assertThat(session.getMorale()).isEqualTo(95);
        assertThat(result.dynamicNote()).isEqualTo("50/50 resolved positive");
    }

    @Test
    void mutinyRandomNegativeSubtractsMorale() {
        SpecialEffectResolver resolver = resolver(false);
        GameSessionEntity session = session();

        SpecialEffectResolver.SpecialEffectResult result =
                resolver.resolve(session, event(11L), choice(14L, SpecialEffectType.RANDOM_5050));

        assertThat(session.getMorale()).isEqualTo(70);
        assertThat(result.dynamicNote()).isEqualTo("50/50 resolved negative");
    }

    @Test
    void sandHillPositiveAddsCashAndCustomers() {
        SpecialEffectResolver resolver = resolver(true);
        GameSessionEntity session = session();

        SpecialEffectResolver.SpecialEffectResult result =
                resolver.resolve(session, event(17L), choice(21L, SpecialEffectType.RANDOM_5050));

        assertThat(session.getCash()).isEqualTo(12_000);
        assertThat(session.getCustomers()).isEqualTo(9);
        assertThat(result.dynamicNote()).isEqualTo("50/50 resolved positive");
    }

    @Test
    void sandHillNegativeSubtractsMorale() {
        SpecialEffectResolver resolver = resolver(false);
        GameSessionEntity session = session();

        SpecialEffectResolver.SpecialEffectResult result =
                resolver.resolve(session, event(17L), choice(21L, SpecialEffectType.RANDOM_5050));

        assertThat(session.getMorale()).isEqualTo(65);
        assertThat(result.dynamicNote()).isEqualTo("50/50 resolved negative");
    }

    @Test
    void linkedinBonusActivatesFlag() {
        SpecialEffectResolver resolver = resolver(true);
        GameSessionEntity session = session();

        SpecialEffectResolver.SpecialEffectResult result =
                resolver.resolve(session, event(14L), choice(16L, SpecialEffectType.LINKEDIN_BONUS));

        assertThat(session.isLinkedinBonusActive()).isTrue();
        assertThat(result.triggeredGameOver()).isFalse();
        assertThat(result.dynamicNote()).isEqualTo("LinkedIn bonus activated");
    }

    @Test
    void nullSpecialEffectIsNoOp() {
        SpecialEffectResolver resolver = resolver(true);
        GameSessionEntity session = session();

        SpecialEffectResolver.SpecialEffectResult result =
                resolver.resolve(session, event(1L), choice(1L, null));

        assertThat(session.getCash()).isEqualTo(8_000);
        assertThat(session.getCustomers()).isEqualTo(5);
        assertThat(session.getMorale()).isEqualTo(80);
        assertThat(result.triggeredGameOver()).isFalse();
        assertThat(result.dynamicNote()).isNull();
    }

    private static SpecialEffectResolver resolver(boolean outcome) {
        return new SpecialEffectResolver(new RandomProvider() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }

            @Override
            public boolean nextBoolean() {
                return outcome;
            }

            @Override
            public double nextDouble(double origin, double bound) {
                return 0;
            }
        });
    }

    private static GameSessionEntity session() {
        GameSessionEntity session = GameSessionEntity.create();
        session.setCash(8_000);
        session.setCustomers(5);
        session.setMorale(80);
        session.setCoffee(10);
        session.setBugs(0);
        session.setStatus(GameSessionStatus.IN_PROGRESS);
        return session;
    }

    private static EventEntity event(Long id) {
        EventEntity event = BeanUtils.instantiateClass(EventEntity.class);
        event.setId(id);
        return event;
    }

    private static EventChoiceEntity choice(Long id, SpecialEffectType specialEffect) {
        EventChoiceEntity choice = BeanUtils.instantiateClass(EventChoiceEntity.class);
        choice.setId(id);
        choice.setSpecialEffect(specialEffect);
        return choice;
    }
}

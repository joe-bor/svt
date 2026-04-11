package com.joe_bor.svt_api.services.turn;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.services.random.RandomProvider;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class ConditionalEventEvaluatorTest {

    @Test
    void bugCrisisFiresWhenProbabilityRollSucceeds() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.4));
        GameSessionEntity session = session();
        session.setBugs(5);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).containsExactly(13L);
    }

    @Test
    void bugCrisisDoesNotFireWhenProbabilityRollMisses() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.6));
        GameSessionEntity session = session();
        session.setBugs(5);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).isEmpty();
    }

    @Test
    void bugCrisisNeverFiresWithZeroBugs() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.0));
        GameSessionEntity session = session();
        session.setBugs(0);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).isEmpty();
    }

    @Test
    void burnoutFiresWhenCoffeeHitsZeroAndFlagIsArmed() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setCoffee(0);
        session.setBurnoutReady(true);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).containsExactly(12L);
        assertThat(session.isBurnoutReady()).isFalse();
    }

    @Test
    void burnoutDoesNotFireWhenEdgeFlagIsNotArmed() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setCoffee(0);
        session.setBurnoutReady(false);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).isEmpty();
        assertThat(session.isBurnoutReady()).isFalse();
    }

    @Test
    void burnoutReArmsWhenCoffeeRecovers() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setCoffee(2);
        session.setBurnoutReady(false);

        evaluator.evaluateAndRoll(session);

        assertThat(session.isBurnoutReady()).isTrue();
        assertThat(session.getPendingEventIds()).isEmpty();
    }

    @Test
    void mutinyFiresWhenMoraleDropsBelowThresholdAndFlagIsArmed() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setMorale(24);
        session.setMutinyReady(true);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).containsExactly(11L);
        assertThat(session.isMutinyReady()).isFalse();
    }

    @Test
    void mutinyDoesNotFireAtThresholdBoundary() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setMorale(25);
        session.setMutinyReady(true);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).isEmpty();
        assertThat(session.isMutinyReady()).isTrue();
    }

    @Test
    void mutinyReArmsWhenMoraleRecovers() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setMorale(40);
        session.setMutinyReady(false);

        evaluator.evaluateAndRoll(session);

        assertThat(session.isMutinyReady()).isTrue();
        assertThat(session.getPendingEventIds()).isEmpty();
    }

    @Test
    void onlyOneConditionalFiresPerTurnAndBugCrisisWinsFirst() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.4));
        GameSessionEntity session = session();
        session.setBugs(5);
        session.setCoffee(0);
        session.setBurnoutReady(true);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).containsExactly(13L);
        assertThat(session.isBurnoutReady()).isTrue();
    }

    @Test
    void burnoutBeatsMutinyWhenBothQualify() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setCoffee(0);
        session.setBurnoutReady(true);
        session.setMorale(24);
        session.setMutinyReady(true);

        evaluator.evaluateAndRoll(session);

        assertThat(session.getPendingEventIds()).containsExactly(12L);
        assertThat(session.isBurnoutReady()).isFalse();
        assertThat(session.isMutinyReady()).isTrue();
    }

    @Test
    void reArmHappensBeforeChecks() {
        ConditionalEventEvaluator evaluator = evaluator(randomProvider(0.9));
        GameSessionEntity session = session();
        session.setCoffee(2);
        session.setBurnoutReady(false);

        evaluator.evaluateAndRoll(session);

        assertThat(session.isBurnoutReady()).isTrue();
        assertThat(session.getPendingEventIds()).isEmpty();
    }

    private static ConditionalEventEvaluator evaluator(RandomProvider randomProvider) {
        return new ConditionalEventEvaluator(eventRepository(), randomProvider);
    }

    private static GameSessionEntity session() {
        GameSessionEntity session = GameSessionEntity.create();
        session.setBugs(0);
        session.setCoffee(5);
        session.setMorale(80);
        session.setBurnoutReady(true);
        session.setMutinyReady(true);
        return session;
    }

    @SuppressWarnings("unchecked")
    private static EventRepository eventRepository() {
        Map<Long, EventEntity> events = Map.of(
                11L, event(11L),
                12L, event(12L),
                13L, event(13L)
        );
        return (EventRepository) Proxy.newProxyInstance(
                ConditionalEventEvaluatorTest.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.ofNullable(events.get(args[0]));
                    case "toString" -> "ConditionalEventEvaluatorTestEventRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static EventEntity event(Long id) {
        EventEntity event = BeanUtils.instantiateClass(EventEntity.class);
        event.setId(id);
        return event;
    }

    private static RandomProvider randomProvider(double... doubles) {
        return new RandomProvider() {
            private int index;

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
                return doubles[index++];
            }
        };
    }
}

package com.joe_bor.svt_api.services.turn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.EventType;
import com.joe_bor.svt_api.models.location.LocationEntity;
import com.joe_bor.svt_api.models.session.GameSessionEntity;
import com.joe_bor.svt_api.repositories.event.EventRepository;
import com.joe_bor.svt_api.services.random.RandomProvider;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

class EventRollServiceTest {

    @Test
    void randomEventAlwaysRollsExactlyOne() {
        EventRollService service = new EventRollService(
                eventRepository(randomEvents(), Map.of()),
                randomProvider(2),
                new ConditionalEventEvaluator(eventRepository(List.of(), Map.of()), randomProvider())
        );
        GameSessionEntity session = sessionAt(location(1L, "San Jose"));

        service.rollEvents(session);

        assertThat(session.getPendingEventIds()).containsExactly(3L);
        assertThat(session.getFiredLocationEventIds()).isEmpty();
    }

    @Test
    void locationEventFiresOnFirstArrivalAtSunnyvale() {
        EventRollService service = new EventRollService(
                eventRepository(randomEvents(), Map.of(3L, event(14L, EventType.LOCATION, location(3L, "Sunnyvale")))),
                randomProvider(0),
                new ConditionalEventEvaluator(eventRepository(List.of(), Map.of()), randomProvider())
        );
        GameSessionEntity session = sessionAt(location(3L, "Sunnyvale"));

        service.rollEvents(session);

        assertThat(session.getPendingEventIds()).containsExactlyInAnyOrder(1L, 14L);
        assertThat(session.getFiredLocationEventIds()).containsExactly(14L);
    }

    @Test
    void locationEventDoesNotFireWhenAlreadyRecorded() {
        EventRollService service = new EventRollService(
                eventRepository(randomEvents(), Map.of(3L, event(14L, EventType.LOCATION, location(3L, "Sunnyvale")))),
                randomProvider(1),
                new ConditionalEventEvaluator(eventRepository(List.of(), Map.of()), randomProvider())
        );
        GameSessionEntity session = sessionAt(location(3L, "Sunnyvale"));
        session.getFiredLocationEventIds().add(14L);

        service.rollEvents(session);

        assertThat(session.getPendingEventIds()).containsExactly(2L);
        assertThat(session.getFiredLocationEventIds()).containsExactly(14L);
    }

    @Test
    void noLocationEventIsAddedWhenCurrentLocationHasNone() {
        EventRollService service = new EventRollService(
                eventRepository(randomEvents(), Map.of()),
                randomProvider(4),
                new ConditionalEventEvaluator(eventRepository(List.of(), Map.of()), randomProvider())
        );
        GameSessionEntity session = sessionAt(location(1L, "San Jose"));

        service.rollEvents(session);

        assertThat(session.getPendingEventIds()).containsExactly(5L);
        assertThat(session.getFiredLocationEventIds()).isEmpty();
    }

    @Test
    void nonEmptyPendingEventsAreRejectedWithoutReRolling() {
        EventRollService service = new EventRollService(
                eventRepository(randomEvents(), Map.of(3L, event(14L, EventType.LOCATION, location(3L, "Sunnyvale")))),
                randomProvider(0),
                new ConditionalEventEvaluator(eventRepository(List.of(), Map.of()), randomProvider())
        );
        GameSessionEntity session = sessionAt(location(3L, "Sunnyvale"));
        session.getPendingEventIds().add(99L);

        assertThatThrownBy(() -> service.rollEvents(session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Pending events must be empty before rolling");

        assertThat(session.getPendingEventIds()).containsExactly(99L);
        assertThat(session.getFiredLocationEventIds()).isEmpty();
    }

    private static List<EventEntity> randomEvents() {
        return java.util.stream.LongStream.rangeClosed(1, 10)
                .mapToObj(id -> event(id, EventType.RANDOM, null))
                .toList();
    }

    private static GameSessionEntity sessionAt(LocationEntity location) {
        GameSessionEntity session = GameSessionEntity.create();
        session.setCurrentLocation(location);
        session.setBugs(0);
        session.setCoffee(5);
        session.setMorale(80);
        session.setBurnoutReady(true);
        session.setMutinyReady(true);
        return session;
    }

    private static EventEntity event(Long id, EventType type, LocationEntity location) {
        EventEntity event = BeanUtils.instantiateClass(EventEntity.class);
        event.setId(id);
        event.setEventType(type);
        event.setLocation(location);
        return event;
    }

    private static LocationEntity location(Long id, String name) {
        LocationEntity location = BeanUtils.instantiateClass(LocationEntity.class);
        location.setId(id);
        location.setName(name);
        return location;
    }

    @SuppressWarnings("unchecked")
    private static EventRepository eventRepository(
            List<EventEntity> randomEvents,
            Map<Long, EventEntity> locationEventsByLocationId
    ) {
        return (EventRepository) Proxy.newProxyInstance(
                EventRollServiceTest.class.getClassLoader(),
                new Class<?>[]{EventRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByEventTypeOrderByIdAsc" -> {
                        EventType type = (EventType) args[0];
                        yield type == EventType.RANDOM ? randomEvents : List.of();
                    }
                    case "findByEventTypeAndLocation" -> {
                        LocationEntity location = (LocationEntity) args[1];
                        yield Optional.ofNullable(locationEventsByLocationId.get(location.getId()));
                    }
                    case "toString" -> "EventRollServiceTestEventRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static RandomProvider randomProvider(int... ints) {
        return new StubRandomProvider(ints, new boolean[0], new double[0]);
    }

    private static RandomProvider randomProvider() {
        return new StubRandomProvider(new int[0], new boolean[0], new double[0]);
    }

    private static final class StubRandomProvider implements RandomProvider {

        private final int[] ints;
        private final boolean[] booleans;
        private final double[] doubles;
        private int intIndex;
        private int booleanIndex;
        private int doubleIndex;

        private StubRandomProvider(int[] ints, boolean[] booleans, double[] doubles) {
            this.ints = ints;
            this.booleans = booleans;
            this.doubles = doubles;
        }

        @Override
        public int nextInt(int bound) {
            return ints[intIndex++];
        }

        @Override
        public boolean nextBoolean() {
            return booleans[booleanIndex++];
        }

        @Override
        public double nextDouble(double origin, double bound) {
            return doubles[doubleIndex++];
        }
    }
}

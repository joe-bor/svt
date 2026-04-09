package com.joe_bor.svt_api.repositories.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.joe_bor.svt_api.models.event.EventEntity;
import com.joe_bor.svt_api.models.event.EventType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.environment=test")
class EventRepositoryIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventChoiceRepository eventChoiceRepository;

    @Test
    void seededEventAndChoiceCountsAreExpected() {
        assertThat(eventRepository.count()).isEqualTo(18);
        assertThat(eventChoiceRepository.count()).isEqualTo(24);
    }

    @Test
    void locationEventsAlwaysHaveLocationAndOthersDoNot() {
        List<EventEntity> events = eventRepository.findAllByOrderByIdAsc();

        assertThat(events).hasSize(18);

        assertThat(events)
                .filteredOn(event -> event.getEventType() == EventType.LOCATION)
                .allMatch(event -> event.getLocation() != null);

        assertThat(events)
                .filteredOn(event -> event.getEventType() != EventType.LOCATION)
                .allMatch(event -> event.getLocation() == null);
    }
}

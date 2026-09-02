package faang.school.postservice.model.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostCreatedEventTest {

    @Test
    void constructorCopiesSubscriberIdsAndReturnsAnImmutableList() {
        List<Long> source = new ArrayList<>(List.of(1L));

        PostCreatedEvent event = new PostCreatedEvent(10L, 20L, source, 1, 1);
        source.add(2L);

        assertThat(event.subscriberIds()).containsExactly(1L);
        assertThatThrownBy(() -> event.subscriberIds().add(3L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

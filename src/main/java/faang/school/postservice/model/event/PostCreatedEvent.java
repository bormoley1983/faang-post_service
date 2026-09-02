package faang.school.postservice.model.event;

import lombok.Builder;

import java.util.List;

@Builder
public record PostCreatedEvent(Long postId, Long authorId, List<Long> subscriberIds,
                               int batchNumber, int totalBatches) {
    public PostCreatedEvent {
        subscriberIds = List.copyOf(subscriberIds);
    }
}

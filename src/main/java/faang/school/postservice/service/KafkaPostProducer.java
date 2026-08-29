package faang.school.postservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Setter
public class KafkaPostProducer {
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topics.publish-post-topic.name}")
    private String topic;

    @Value("${spring.kafka.topics.publish-post-topic.subscribers-batch-size:1000}")
    private int batchSize;

    public void publishPostCreationEvent(Post post) {
        List<Long> allSubscribers = fetchSubscriberIds(post.getAuthorId());
        if (allSubscribers.isEmpty()) {
            return;
        }

        List<List<Long>> batches = partitionSubscriberIds(allSubscribers);
        int batchesCount = batches.size();

        IntStream.range(0, batchesCount)
                .forEach(currentBatch -> {
                    List<Long> subscriberBatch = batches.get(currentBatch);
                    PostCreatedEvent event = createPostCreatedEvent(
                            post,
                            subscriberBatch,
                            currentBatch + 1,
                            batchesCount
                    );
                    sendEvent(event);
                });
    }

    private List<Long> fetchSubscriberIds(Long authorId) {
        return userServiceClient.getFollowers(authorId).stream()
                .map(UserDto::id)
                .toList();
    }

    private List<List<Long>> partitionSubscriberIds(List<Long> allSubscribers) {
        return ListUtils.partition(allSubscribers, batchSize);
    }

    private PostCreatedEvent createPostCreatedEvent(Post post, List<Long> subscriberBatch,
                                                    int currentBatch, int totalBatches) {
        return new PostCreatedEvent(
                post.getId(),
                post.getAuthorId(),
                subscriberBatch,
                currentBatch,
                totalBatches
        );
    }

    private void sendEvent(PostCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, eventJson);
            log.info("Published post creation batch: postId={}, authorId={}, batchSize={}, batchNumber={}",
                    event.postId(), event.authorId(), event.subscriberIds().size(), event.batchNumber());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize post event batch: {}", e.getMessage());
            throw new RuntimeException("Failed to publish post creation event batch", e);
        }
    }
}

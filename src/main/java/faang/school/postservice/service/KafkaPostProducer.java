package faang.school.postservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.PostCreatedEvent;
import faang.school.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Setter
public class KafkaPostProducer {
    private final PostRepository postRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.topics.publish-post-topic.name}")
    private String topic;

    @Value("${spring.kafka.topics.publish-post-topic.subscribers-batch-size:1000}")
    private int batchSize;

    public void publishPostCreationEvent(Post post) {
        int batchesCount = countSubscribersPages(post.getAuthorId());
        List<Long> allSubscribers = fetchSubscriberIds(post.getAuthorId(), batchesCount);
        List<List<Long>> batches = partitionSubscriberIds(allSubscribers);

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

    private int countSubscribersPages(Long authorId) {
        Long subscribersCount = postRepository.findAuthorSubscribersCount(authorId);
        return (int) Math.ceil((double) subscribersCount / batchSize);
    }

    private List<Long> fetchSubscriberIds(Long authorId, int pagesCount) {
        List<Long> result = IntStream.range(0, pagesCount)
                .mapToObj( page -> {
                    Pageable pageable = PageRequest.of(page, batchSize);
                    return postRepository.findAuthorSubscribers(authorId, pageable);
                })
                .flatMap(List::stream)
                .toList();

        return result;
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

package faang.school.postservice.publisher.like;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.AnalyticsTopicsProperties;
import faang.school.postservice.mapper.LikeMapper;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.AnalyticsLikeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class AnalyticsLikeEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LikeMapper likeMapper;

    @Mock
    private AnalyticsTopicsProperties analyticsTopics;

    @InjectMocks
    private AnalyticsLikeEventPublisher analyticsLikeEventPublisher;

    @Test
    void publishEvent_whenSerializationSucceeds_sendsJsonToConfiguredTopic() throws JsonProcessingException {
        // Arrange
        Post post = new Post();
        post.setAuthorId(42L);
        Like like = new Like();
        like.setPost(post);
        AnalyticsLikeEvent event = new AnalyticsLikeEvent();
        String json = "{\"like\":1}";

        when(analyticsTopics.like()).thenReturn("test-analytics-like-topic");
        when(likeMapper.toAnalyticsLikeEvent(like)).thenReturn(event);
        when(objectMapper.writeValueAsString(event)).thenReturn(json);

        // Act
        analyticsLikeEventPublisher.publishEvent(like);

        // Assert
        verify(likeMapper).toAnalyticsLikeEvent(like);
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send("test-analytics-like-topic", json);
    }

    @Test
    void publishEvent_whenSerializationFails_logsErrorAndDoesNotThrow() throws JsonProcessingException {
        // Arrange
        Post post = new Post();
        post.setAuthorId(42L);
        Like like = new Like();
        like.setPost(post);
        AnalyticsLikeEvent event = new AnalyticsLikeEvent();

        when(likeMapper.toAnalyticsLikeEvent(like)).thenReturn(event);
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Error writing JSON") {});

        // Act & Assert
        assertThatCode(() -> analyticsLikeEventPublisher.publishEvent(like))
                .doesNotThrowAnyException();
        verifyNoInteractions(kafkaTemplate);
    }
}

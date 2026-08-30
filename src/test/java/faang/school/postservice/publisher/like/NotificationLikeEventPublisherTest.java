package faang.school.postservice.publisher.like;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.mapper.LikeMapper;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.NotificationLikeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationLikeEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private LikeMapper likeMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationLikeEventPublisher notificationLikeEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                notificationLikeEventPublisher,
                "notificationLikeTopic",
                "test-notification-like-topic"
        );
    }

    @Test
    void publishEvent_whenSerializationSucceeds_sendsJsonToKafka() throws JsonProcessingException {
        // Arrange
        Post post = new Post();
        post.setAuthorId(42L);
        Like like = new Like();
        like.setPost(post);
        NotificationLikeEvent event = new NotificationLikeEvent();
        String json = "{\"like\":1}";

        when(likeMapper.toNotificationLikeEvent(like)).thenReturn(event);
        when(objectMapper.writeValueAsString(event)).thenReturn(json);

        // Act
        notificationLikeEventPublisher.publishEvent(like);

        // Assert
        verify(likeMapper).toNotificationLikeEvent(like);
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send("test-notification-like-topic", json);
    }

    @Test
    void publishEvent_whenSerializationFails_logsErrorAndDoesNotThrow() throws JsonProcessingException {
        // Arrange
        Post post = new Post();
        post.setAuthorId(42L);
        Like like = new Like();
        like.setPost(post);
        NotificationLikeEvent event = new NotificationLikeEvent();

        when(likeMapper.toNotificationLikeEvent(like)).thenReturn(event);
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Error writing JSON") {});

        // Act & Assert
        assertThatCode(() -> notificationLikeEventPublisher.publishEvent(like))
                .doesNotThrowAnyException();
        verifyNoInteractions(kafkaTemplate);
    }
}

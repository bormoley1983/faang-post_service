package faang.school.postservice.publisher.post;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.exception.EventSerializationException;
import faang.school.postservice.mapper.PostViewMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.PostViewEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostViewPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PostViewMapper postViewMapper;

    @InjectMocks
    private PostViewPublisher postViewPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postViewPublisher, "postViewTopicName", "test-post-view-topic");
    }

    @Test
    void publishEvent_whenSerializationSucceeds_sendsJsonToKafka() throws JsonProcessingException {
        // Arrange
        Post post = new Post();
        PostViewEvent event = new PostViewEvent();
        String json = "{\"post\":1}";

        when(postViewMapper.toEvent(post)).thenReturn(event);
        when(objectMapper.writeValueAsString(event)).thenReturn(json);

        // Act
        postViewPublisher.publishEvent(post);

        // Assert
        verify(postViewMapper).toEvent(post);
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send("test-post-view-topic", json);
    }

    @Test
    void publishEvent_whenSerializationFails_throwsEventSerializationException() throws JsonProcessingException {
        // Arrange
        Post post = new Post();
        PostViewEvent event = new PostViewEvent();

        when(postViewMapper.toEvent(post)).thenReturn(event);
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Error writing JSON") {});

        // Act & Assert
        assertThrows(EventSerializationException.class, () -> postViewPublisher.publishEvent(post));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void publishEvent_whenKafkaSendFails_throwsEventSerializationException() throws JsonProcessingException {
        // Arrange
        Post post = new Post();
        PostViewEvent event = new PostViewEvent();

        when(postViewMapper.toEvent(post)).thenReturn(event);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"post\":1}");
        org.mockito.Mockito.doThrow(new RuntimeException("kafka down"))
                .when(kafkaTemplate).send("test-post-view-topic", "{\"post\":1}");

        // Act & Assert
        assertThatCode(() -> postViewPublisher.publishEvent(post))
                .isInstanceOf(RuntimeException.class);
    }
}

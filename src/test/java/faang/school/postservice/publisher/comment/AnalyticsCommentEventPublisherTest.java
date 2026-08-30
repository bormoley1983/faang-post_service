package faang.school.postservice.publisher.comment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.event.AnalyticsCommentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnalyticsCommentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalyticsCommentEventPublisher analyticsCommentEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                analyticsCommentEventPublisher,
                "analyticsCommentTopicName",
                "test-analytics-comment-topic"
        );
    }

    @Test
    void publishEvent_whenSerializationSucceeds_sendsJsonToKafka() throws JsonProcessingException {
        // Arrange
        Comment comment = new Comment();
        AnalyticsCommentEvent event = new AnalyticsCommentEvent();
        String json = "{\"comment\":1}";

        when(commentMapper.toAnalyticsCommentEvent(comment)).thenReturn(event);
        when(objectMapper.writeValueAsString(event)).thenReturn(json);

        // Act
        analyticsCommentEventPublisher.publishEvent(comment);

        // Assert
        verify(commentMapper).toAnalyticsCommentEvent(comment);
        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send("test-analytics-comment-topic", json);
    }

    @Test
    void publishEvent_whenSerializationFails_logsErrorAndDoesNotThrow() throws JsonProcessingException {
        // Arrange
        Comment comment = new Comment();
        AnalyticsCommentEvent event = new AnalyticsCommentEvent();

        when(commentMapper.toAnalyticsCommentEvent(comment)).thenReturn(event);
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Error writing JSON") {});

        // Act & Assert
        assertThatCode(() -> analyticsCommentEventPublisher.publishEvent(comment))
                .doesNotThrowAnyException();
        verifyNoInteractions(kafkaTemplate);
    }
}

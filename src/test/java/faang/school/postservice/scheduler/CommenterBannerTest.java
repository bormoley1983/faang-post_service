package faang.school.postservice.scheduler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.exception.EventSerializationException;
import faang.school.postservice.model.event.UserBanEvent;
import faang.school.postservice.service.CommentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommenterBannerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CommenterBanner commenterBanner;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(commenterBanner, "userBanTopicName", "test-user-ban-topic");
    }

    @Test
    void runBannerTask_whenAuthorIdsFound_sendsSerializedBanEventPerId() throws JsonProcessingException {
        // Arrange
        List<Long> authorIds = List.of(10L, 20L);
        when(commentService.findAuthorIdsForBan()).thenReturn(authorIds);
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any(UserBanEvent.class)))
                .thenReturn("{\"userId\":10,\"banned\":true}");

        // Act
        commenterBanner.runBannerTask();

        // Assert
        verify(kafkaTemplate, org.mockito.Mockito.times(2)).send("test-user-ban-topic", "{\"userId\":10,\"banned\":true}");
    }

    @Test
    void runBannerTask_whenNoAuthorIdsFound_doesNotSendEvents() {
        // Arrange
        when(commentService.findAuthorIdsForBan()).thenReturn(List.of());

        // Act
        commenterBanner.runBannerTask();

        // Assert
        verify(kafkaTemplate, never()).send(anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void runBannerTask_whenSerializationFails_throwsEventSerializationException() throws JsonProcessingException {
        // Arrange
        List<Long> authorIds = List.of(10L);
        when(commentService.findAuthorIdsForBan()).thenReturn(authorIds);
        when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any(UserBanEvent.class)))
                .thenThrow(new JsonProcessingException("Error writing JSON") {});

        // Act & Assert
        assertThrows(EventSerializationException.class, () -> commenterBanner.runBannerTask());
        verify(kafkaTemplate, never()).send(anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}

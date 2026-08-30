package faang.school.postservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.exception.EventSerializationException;
import faang.school.postservice.model.event.UserBanEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class BanProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BanProducer banProducer;

    @Test
    void sendUserToBan_whenSerializationSucceeds_sendsJsonToKafka() throws JsonProcessingException {
        // Arrange
        UserBanEvent event = new UserBanEvent(1L, true);
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"userId\":1,\"banned\":true}");

        // Act
        banProducer.sendUserToBan("test-user-ban-topic", event);

        // Assert
        verify(kafkaTemplate).send("test-user-ban-topic", "{\"userId\":1,\"banned\":true}");
    }

    @Test
    void sendUserToBan_whenSerializationFails_throwsEventSerializationException() throws JsonProcessingException {
        // Arrange
        UserBanEvent event = new UserBanEvent(1L, true);
        when(objectMapper.writeValueAsString(event))
                .thenThrow(new JsonProcessingException("Error writing JSON") {});

        // Act & Assert
        assertThrows(EventSerializationException.class,
                () -> banProducer.sendUserToBan("test-user-ban-topic", event));
        verify(kafkaTemplate, never()).send(anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}

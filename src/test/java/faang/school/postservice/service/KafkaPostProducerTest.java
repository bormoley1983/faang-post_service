package faang.school.postservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.PostCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KafkaPostProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
        private UserServiceClient userServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaPostProducer kafkaPostProducer;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @BeforeEach
    void setUp() {
        kafkaPostProducer.setTopic("publish_post_topic");
        kafkaPostProducer.setBatchSize(2);
    }

    @Test
    void shouldPublishPostCreationEvent() throws JsonProcessingException {
        Long postId = 1L;
        Long authorId = 2L;
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(authorId);

        List<UserDto> subscribers = List.of(
                new UserDto(3L, "u3", "u3@mail.com"),
                new UserDto(4L, "u4", "u4@mail.com"),
                new UserDto(5L, "u5", "u5@mail.com")
        );
        when(userServiceClient.getFollowers(eq(authorId))).thenReturn(subscribers);
        when(objectMapper.writeValueAsString(any(PostCreatedEvent.class)))
                .thenReturn("{\"json\":\"content\"}");

        kafkaPostProducer.publishPostCreationEvent(post);

        verify(kafkaTemplate, times(2)).send(eq("publish_post_topic"), messageCaptor.capture());
        verify(objectMapper, times(2)).writeValueAsString(any(PostCreatedEvent.class));
                verify(userServiceClient, times(1)).getFollowers(eq(authorId));
        }

    @Test
    void shouldHandleJsonProcessingException() throws JsonProcessingException {
        Long authorId = 2L;
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(authorId);

        List<UserDto> subscribers = List.of(
                new UserDto(3L, "u3", "u3@mail.com"),
                new UserDto(4L, "u4", "u4@mail.com"),
                new UserDto(5L, "u5", "u5@mail.com")
        );
        when(userServiceClient.getFollowers(eq(authorId))).thenReturn(subscribers);
        when(objectMapper.writeValueAsString(any(PostCreatedEvent.class)))
                .thenThrow(new JsonProcessingException("Error processing JSON") {});

        assertThrows(RuntimeException.class,
                () -> kafkaPostProducer.publishPostCreationEvent(post));

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
package faang.school.postservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.PostCreatedEvent;
import faang.school.postservice.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
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
    PostRepository postRepository;

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

        List<Long> subscriberIds = List.of(3L, 4L, 5L);
        when(postRepository.findAuthorSubscribersCount(eq(authorId))).thenReturn(3L);
        when(postRepository.findAuthorSubscribers(eq(authorId), any(Pageable.class)))
                .thenReturn(subscriberIds.subList(0, 2))
                .thenReturn(subscriberIds.subList(2, 3));
        when(objectMapper.writeValueAsString(any(PostCreatedEvent.class)))
                .thenReturn("{\"json\":\"content\"}");

        kafkaPostProducer.publishPostCreationEvent(post);

        verify(kafkaTemplate, times(2)).send(eq("publish_post_topic"), messageCaptor.capture());
        verify(objectMapper, times(2)).writeValueAsString(any(PostCreatedEvent.class));
        verify(postRepository, times(1)).findAuthorSubscribersCount(eq(authorId));
        verify(postRepository, times(2)).findAuthorSubscribers(eq(authorId), any(Pageable.class));    }

    @Test
    void shouldHandleJsonProcessingException() throws JsonProcessingException {
        Long authorId = 2L;
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(authorId);

        List<Long> subscriberIds = List.of(3L, 4L, 5L);
        when(postRepository.findAuthorSubscribersCount(eq(authorId))).thenReturn(3L);
        when(postRepository.findAuthorSubscribers(eq(authorId), any(Pageable.class)))
                .thenReturn(subscriberIds.subList(0, 2))
                .thenReturn(subscriberIds.subList(2, 3));
        when(objectMapper.writeValueAsString(any(PostCreatedEvent.class)))
                .thenThrow(new JsonProcessingException("Error processing JSON") {});

        assertThrows(RuntimeException.class,
                () -> kafkaPostProducer.publishPostCreationEvent(post));

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
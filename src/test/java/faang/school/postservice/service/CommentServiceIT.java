package faang.school.postservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.kafka.KafkaTestConfig;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.event.AnalyticsCommentEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Tag("integration")
@Import(KafkaTestConfig.class)
@Testcontainers
@SpringBootTest
public class CommentServiceIT {

    
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.7.7");

    static Network testNetwork = Network.newNetwork();

    @Container
    @SuppressWarnings("resource")
    protected static final  ConfluentKafkaContainer KAFKA_CONTAINER = 
        new ConfluentKafkaContainer(KAFKA_IMAGE)
            .withNetwork(testNetwork)
            .withNetworkAliases("test-kafka");

    static {
        KAFKA_CONTAINER.start();
    }  

    @Autowired
    private CommentService commentService;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserServiceClient userServiceClient;

    private static final String analyticsCommentTopicName = "analytics_comment_topic";

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.topics.analytics-comment-topic.name", () -> analyticsCommentTopicName);
    }

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testCreateComment_ShouldPublishAnalyticsCommentEvent() throws IOException {
        Comment comment = Comment.builder()
                .content("Test content")
                .verified(true)
                .build();
        Long postId = 1L;
        Long authorId = 1L;

        when(userServiceClient.getUser(anyLong())).thenReturn(new UserDto(1L, "username", "email"));

        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singleton(analyticsCommentTopicName));

            Comment savedComment = commentService.createComment(comment, postId, authorId);

            AtomicReference<ConsumerRecord<String, String>> recordRef = new AtomicReference<>();

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
                        assertThat(records.isEmpty())
                                .as("No records found in Kafka topic")
                                .isFalse();

                        records.records(analyticsCommentTopicName).forEach(record -> {
                            if (record.value() != null) {
                                recordRef.set(record);
                            }
                        });
                    });

            AnalyticsCommentEvent event = objectMapper.readValue(
                    recordRef.get().value(),
                    AnalyticsCommentEvent.class
            );

            assertThat(event.getCommentId()).isEqualTo(savedComment.getId());
            assertThat(event.getPostId()).isEqualTo(postId);
            assertThat(event.getAuthorId()).isEqualTo(authorId);
        }
    }

    @AfterAll
    static void cleanup() {
        if (KAFKA_CONTAINER != null && KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.stop();
        }
        if (testNetwork != null) {
            testNetwork.close();
        }
    }
}

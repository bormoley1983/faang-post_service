package faang.school.postservice.publisher.comment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.kafka.KafkaTestConfig;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.event.NotificationCommentEvent;
import faang.school.postservice.service.AsyncModerationService;
import faang.school.postservice.util.ModerationDictionaryUtil;
import faang.school.postservice.validation.ModerationDictionaryValidation;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ActiveProfiles("test")
@ContextConfiguration(classes = KafkaTestConfig.class)
@Tag("integration")
@Testcontainers
@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
        }
)
public class NotificationCommentEventPublisherIT {

    @Value("${spring.kafka.topics.notification-comment-topic.name}")
    private String notificationCommentTopicName;

    @Autowired
    private NotificationCommentEventPublisher commentPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Consumer<String, String> consumer;

    @MockitoBean
    private AsyncModerationService asyncModerationService;

    @MockitoBean
    private ModerationDictionaryValidation moderationDictionaryValidation;

    @MockitoBean
    private ModerationDictionaryUtil moderationDictionaryUtil;

    private Post post;
    private Comment comment;

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.7.7");

    static Network testNetwork = Network.newNetwork();

    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer POSTGRESQL_CONTAINER =
        new PostgreSQLContainer(POSTGRES_IMAGE)
            .withNetwork(testNetwork)
            .withNetworkAliases("test-postgres")		        
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    @SuppressWarnings("resource")
    protected static final  ConfluentKafkaContainer KAFKA_CONTAINER = 
        new ConfluentKafkaContainer(KAFKA_IMAGE)
            .withNetwork(testNetwork)
            .withNetworkAliases("test-kafka");

    static {
        POSTGRESQL_CONTAINER.start();
        KAFKA_CONTAINER.start();
    }            

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);
        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);

        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }

    @BeforeEach
    public void setup() {
        post = Post.builder()
                .id(1L).build();
        comment = Comment.builder()
                .id(1L)
                .post(post)
                .content("test-comment")
                .build();
    }

    @Test
    public void testCommentNotificationEventIsSent() throws JsonProcessingException {
        consumer.subscribe(Collections.singletonList(notificationCommentTopicName));

        commentPublisher.publishEvent(comment);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertThat(records.count()).isGreaterThan(0);

        for (ConsumerRecord<String, String> record : records) {
            NotificationCommentEvent event = objectMapper.readValue(record.value(), NotificationCommentEvent.class);
            assertThat(event.getCommentId()).isEqualTo(comment.getId());
        }
    }
}

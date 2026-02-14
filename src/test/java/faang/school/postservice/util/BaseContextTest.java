package faang.school.postservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import faang.school.postservice.PostServiceApp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(
        classes = {
                PostServiceApp.class
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
public class BaseContextTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18-alpine");
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:8-alpine");

    static Network testNetwork = Network.newNetwork();

    @Container
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withNetwork(testNetwork)
            .withNetworkAliases("test-postgres")		        
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    @SuppressWarnings("resource")
    protected static final RedisContainer REDIS_CONTAINER = new RedisContainer(REDIS_IMAGE)
            .withNetwork(testNetwork)
            .withNetworkAliases("test-redis")
            .withReuse(true);

    static {
        POSTGRESQL_CONTAINER.start();
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        POSTGRESQL_CONTAINER.start();
        REDIS_CONTAINER.start();

        registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL_CONTAINER::getPassword);

        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    }

    @AfterAll
    static void cleanup() {
        if (POSTGRESQL_CONTAINER != null && POSTGRESQL_CONTAINER.isRunning()) {
            POSTGRESQL_CONTAINER.stop();
        }
        if (REDIS_CONTAINER != null && REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.stop();
        }
        if (testNetwork != null) {
            testNetwork.close();
        }
    }
}
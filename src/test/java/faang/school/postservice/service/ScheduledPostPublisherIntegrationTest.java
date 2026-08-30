package faang.school.postservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.repository.PostCacheRepository;
import faang.school.postservice.util.BaseContextTest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Tag("integration")
@ActiveProfiles("test")
@RequiredArgsConstructor
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        properties = {
                "spring.redis.host=localhost",
                "spring.redis.port=6379",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
        },

        classes = {ScheduledPostPublisher.class, PostService.class}
)
public class ScheduledPostPublisherIntegrationTest extends BaseContextTest {

    @Autowired
    private ScheduledPostPublisher scheduledPostPublisher;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostCacheRepository postCacheRepository;

    @MockitoBean
    private ObjectMapper objectMapper;

    @Test
    void testPostPublisherSchedule(CapturedOutput output) {
        doNothing().when(postService).publishScheduledPosts();
        scheduledPostPublisher.postPublisherSchedule();
        verify(postService, times(1)).publishScheduledPosts();

        assertTrue(output.getOut().contains("Publish scheduler started processing"));
        assertTrue(output.getOut().contains("Publish scheduler finished processing"));
    }
}

package faang.school.postservice.scheduler;

import static org.mockito.Mockito.verify;

import faang.school.postservice.service.PostCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCacheCleanerTest {

    @Mock
    private PostCacheService postCacheService;

    @InjectMocks
    private PostCacheCleaner postCacheCleaner;

    @Test
    void cleanupExpiredCache_delegatesToPostCacheService() {
        // Act
        postCacheCleaner.cleanupExpiredCache();

        // Assert
        verify(postCacheService).cleanupExpiredPosts();
    }
}

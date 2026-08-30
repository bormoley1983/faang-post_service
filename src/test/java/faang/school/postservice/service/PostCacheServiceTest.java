package faang.school.postservice.service;

import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostCacheServiceTest {
    private PostCacheService postCacheService;

    @Mock
    private RedisTemplate<String, Post> redisTemplate;

    @Mock
    private PostCacheRepository postCacheRepository;

    private static final long DEFAULT_TTL_HOURS = 24;

    @BeforeEach
    void setUp() {
        postCacheService = new PostCacheService(postCacheRepository);
        ReflectionTestUtils.setField(postCacheService, "postTtl", Duration.ofHours(DEFAULT_TTL_HOURS));
    }

    @Test
    void shouldCachePost() {
        Post post = new Post();
        post.setId(1L);
        post.setPublished(true);

        postCacheService.cachePost(post);

        verify(postCacheRepository).save(eq(post), eq(Duration.ofHours(DEFAULT_TTL_HOURS)));
    }

    @Test
    void shouldGetCachedPost() {
        Post post = new Post();
        post.setId(1L);
        when(postCacheRepository.findById(1L)).thenReturn(Optional.of(post));

        Optional<Post> result = postCacheService.getCachedPost(1L);

        assertTrue(result.isPresent());
        assertEquals(post, result.get());
    }

    @Test
    void shouldReturnEmptyWhenPostNotCached() {
        when(postCacheRepository.findById(2L)).thenReturn(Optional.empty());

        Optional<Post> result = postCacheService.getCachedPost(2L);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldRemovePostFromCache() {
        Long postId = 1L;

        postCacheService.removePostFromCache(postId);

        verify(postCacheRepository).deleteById(postId);
    }

    @Test
    void cleanupExpiredPosts_delegatesToRepository() {
        // Act
        postCacheService.cleanupExpiredPosts();

        // Assert
        verify(postCacheRepository).removeExpiredPosts();
    }
}

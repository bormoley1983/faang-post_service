package faang.school.postservice.service;

import faang.school.postservice.client.ProjectServiceClient;
import faang.school.postservice.model.Post;
import faang.school.postservice.publisher.post.PostViewPublisher;
import faang.school.postservice.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PostServiceCacheTest {

    @Mock
    private AsyncModerationService asyncModerationService;

    @Mock
    private SpellCheckerService spellCheckerService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private InternalServices internalServices;

    @Mock
    private PostCacheService postCacheService;

    @Mock
    private KafkaPostProducer kafkaPostProducer;

    @Mock
    private UserCashService userCashService;

    @Mock
    private PostViewPublisher postViewPublisher;

    @Mock
    private ProjectServiceClient projectServiceClient;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "maxQuerySize", 100);
    }

    @Test
    void shouldCachePostAfterPublishing() {
        Long postId = 1L;
        Post post = new Post();
        post.setId(postId);
        post.setAuthorId(1L);
        post.setPublished(false);

        Post publishedPost = new Post();
        publishedPost.setId(postId);
        publishedPost.setPublished(true);
        publishedPost.setPublishedAt(LocalDateTime.now());

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(publishedPost);

        Post result = postService.publish(postId, 1L);

        verify(postCacheService).cachePost(result);
        assertTrue(result.isPublished());
    }

    @Test
    void shouldRemovePostFromCacheWhenDeleted() {
        Long postId = 1L;
        Post post = new Post();
        post.setId(postId);
        post.setDeleted(false);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        post.setAuthorId(1L);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postService.delete(postId, 1L);

        verify(postRepository).save(argThat(Post::isDeleted));
        verify(postCacheService).removePostFromCache(postId);
    }

    @Test
    void shouldReturnFromCacheWhenAvailable() {
        Long postId = 1L;
        Post cachedPost = new Post();
        cachedPost.setId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(new Post()));
        when(postCacheService.getCachedPost(postId)).thenReturn(Optional.of(cachedPost));

        Post result = postService.get(postId);

        verify(postCacheService).getCachedPost(postId);
        verify(postViewPublisher).publishEvent(cachedPost);
        verifyNoInteractions(postRepository);
        assertSame(cachedPost, result);
    }

    @Test
    void shouldGetFromDatabaseAndCacheWhenNotInCache() {
        Long postId = 1L;
        Post dbPost = new Post();
        dbPost.setId(postId);

        when(postCacheService.getCachedPost(postId)).thenReturn(Optional.empty());
        when(postRepository.findById(postId)).thenReturn(Optional.of(dbPost));

        Post result = postService.get(postId);

        verify(postCacheService).getCachedPost(postId);
        verify(postRepository).findById(postId);
        verify(postViewPublisher).publishEvent(dbPost);
        assertSame(dbPost, result);
    }
}

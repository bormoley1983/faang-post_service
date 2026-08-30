package faang.school.postservice.util;

import faang.school.postservice.client.ProjectServiceClient;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.Resource;
import faang.school.postservice.publisher.post.PostViewPublisher;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.AiModerationService;
import faang.school.postservice.service.AsyncModerationService;
import faang.school.postservice.service.InternalServices;
import faang.school.postservice.service.KafkaPostProducer;
import faang.school.postservice.service.PostCacheService;
import faang.school.postservice.service.PostService;
import faang.school.postservice.service.SpellCheckerService;
import faang.school.postservice.service.UserCashService;
import faang.school.postservice.validation.ModerationDictionaryValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @Mock
    private PostRepository postRepository;

    @Mock
    private InternalServices internalServices;

    @Mock
    private ModerationDictionaryValidation moderationDictionaryValidation;

    @Mock
    private AiModerationService aiModerationService;

    @Mock
    private AsyncModerationService asyncModerationService;

    @Mock
    private SpellCheckerService spellCheckerService;

    @Mock
    private KafkaPostProducer kafkaPostProducer;

    @Mock
    private PostCacheService postCacheService;

    @Mock
    private UserCashService userCashService;

    @Mock
    private PostViewPublisher postViewPublisher;

    @Mock
    private ProjectServiceClient projectServiceClient;

    @InjectMocks
    private PostService postService;

    private Post post;
    private Post projectPost;
    private Post originalPost;
    private Post post1;
    private Post post2;
    private List<Post> postsToPublish;
    private List<Post> unpublishedPosts;
    private List<String> contents;
    private List<String> correctedContents;

    @BeforeEach
    public void SetUp() {
        ReflectionTestUtils.setField(postService, "maxQuerySize", 100);
        post = new Post();
        post.setId(1L);
        post.setAuthorId(1L);

        projectPost = new Post();
        projectPost.setId(1L);
        projectPost.setProjectId(1L);

        originalPost = new Post();
        originalPost.setId(1L);
        originalPost.setAuthorId(1L);

        post1 = new Post();
        post1.setId(1L);
        post1.setAuthorId(1L);
        post1.setProjectId(1L);
        post1.setDeleted(false);
        post1.setPublished(false);
        post1.setCreatedAt(LocalDateTime.now().minusDays(1));
        post1.setContent("This is a test post with sme errors.");

        post2 = new Post();
        post2.setId(2L);
        post2.setAuthorId(1L);
        post2.setProjectId(1L);
        post2.setDeleted(false);
        post2.setPublished(false);
        post2.setCreatedAt(LocalDateTime.now());
        post2.setContent("Anothr post with some mistakes.");

        postsToPublish = List.of(post1, post2);

        unpublishedPosts = List.of(post1, post2);

        contents = postsToPublish.stream()
                .map(Post::getContent)
                .toList();

        correctedContents = List.of(
                "This is a test post with some errors.",
                "Another post with some mistakes."
        );

    }

    @Test
    @Order(1)
    public void createDraft_ValidAuthor() {
        when(internalServices.userExists(1L)).thenReturn(true);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        Post result = postService.createDraft(post, 1L);

        assertNotNull(result);
    }

    @Test
    @Order(2)
    public void createDraft_InvalidAuthor() {
        when(internalServices.userExists(1L)).thenReturn(false);

        assertThrows(InvalidParameterException.class, () -> postService.createDraft(post, 1L));
    }

    @Test
    @Order(3)
    public void createDraft_InvalidProject() {
        when(internalServices.projectExists(1L)).thenReturn(false);

        assertThrows(InvalidParameterException.class, () -> postService.createDraft(projectPost, 1L));
    }

    @Test
    @Order(4)
    public void publish_Valid() {
        post.setPublished(false);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        Post result = postService.publish(1L, 1L);

        assertTrue(result.isPublished());
        assertNotNull(result.getPublishedAt());
    }

    @Test
    @Order(5)
    public void publish_PostNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DataValidationException.class, () -> postService.publish(1L, 1L));
    }

    @Test
    @Order(6)
    public void publish_AlreadyPublished() {
        post.setPublished(true);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(DataValidationException.class, () -> postService.publish(1L, 1L));
    }

    @Test
    public void publish_NotOwner() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        assertThrows(DataValidationException.class, () -> postService.publish(1L, 2L));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @Order(7)
    public void update_Valid() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(originalPost));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        Post result = postService.update(post, 1L);

        assertNotNull(result);
    }

    @Test
    @Order(8)
    public void update_PostNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DataValidationException.class, () -> postService.update(post, 1L));
    }

    @Test
    @Order(9)
    public void update_IgnoresCallerSuppliedAuthor() {
        post.setAuthorId(2L);

        when(postRepository.findById(1L)).thenReturn(Optional.of(originalPost));
        when(postRepository.save(any(Post.class))).thenReturn(originalPost);

        assertDoesNotThrow(() -> postService.update(post, 1L));
        assertEquals(1L, originalPost.getAuthorId());
    }

    @Test
    public void addResources_PersistsResourcesForOwner() {
        Resource resource = Resource.builder().key("post/1/resource").build();
        when(postRepository.findById(1L)).thenReturn(Optional.of(originalPost));
        when(postRepository.save(originalPost)).thenReturn(originalPost);

        Post result = postService.addResources(1L, List.of(resource), 1L);

        assertSame(originalPost, result);
        assertTrue(originalPost.getResources().contains(resource));
        assertSame(originalPost, resource.getPost());
        verify(postRepository).save(originalPost);
    }

    @Test
    @Order(10)
    public void delete_Valid() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        assertDoesNotThrow(() -> postService.delete(1L, 1L));
    }

    @Test
    @Order(11)
    public void delete_PostNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DataValidationException.class, () -> postService.delete(1L, 1L));
    }

    @Test
    @Order(12)
    public void get_ValidPost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Post result = postService.get(1L);

        assertNotNull(result);
    }

    @Test
    @Order(13)
    public void get_PostNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DataValidationException.class, () -> postService.get(1L));
    }

    @Test
    @Order(14)
    public void getDraftsByAuthorId_Valid() {
        when(postRepository.findByAuthorIdAndDeletedFalseAndPublishedFalseOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(post2, post1), PageRequest.of(0, 100), 2));

        List<Post> result = postService.getDraftsByAuthorId(1L);

        assertEquals(2, result.size());
        assertEquals(post2, result.get(0));
        assertEquals(post1, result.get(1));
    }

    @Test
    @Order(15)
    public void getDraftsByProjectId_Valid() {
        when(postRepository.findByProjectIdAndDeletedFalseAndPublishedFalseOrderByCreatedAtDesc(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(post2, post1), PageRequest.of(0, 100), 2));

        List<Post> result = postService.getDraftsByProjectId(1L);

        assertEquals(2, result.size());
        assertEquals(post2, result.get(0));
        assertEquals(post1, result.get(1));
    }

    @Test
    @Order(16)
    public void getPostsByAuthorId_Valid() {
        post1.setPublished(true);
        post1.setPublishedAt(LocalDateTime.now().minusDays(1));
        post2.setPublished(true);
        post2.setPublishedAt(LocalDateTime.now());

        when(postRepository.findByAuthorIdAndDeletedFalseAndPublishedTrueOrderByPublishedAtDesc(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(post2, post1), PageRequest.of(0, 100), 2));

        List<Post> result = postService.getPostsByAuthorId(1L);

        assertEquals(2, result.size());
        assertEquals(post2, result.get(0));
        assertEquals(post1, result.get(1));
    }

    @Test
    @Order(17)
    public void getPostsByProjectId_Valid() {
        post1.setPublished(true);
        post1.setPublishedAt(LocalDateTime.now().minusDays(1));
        post2.setPublished(true);
        post2.setPublishedAt(LocalDateTime.now());

        when(postRepository.findByProjectIdAndDeletedFalseAndPublishedTrueOrderByPublishedAtDesc(anyLong(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(post2, post1), PageRequest.of(0, 100), 2));

        List<Post> result = postService.getPostsByProjectId(1L);

        assertEquals(2, result.size());
        assertEquals(post2, result.get(0));
        assertEquals(post1, result.get(1));
    }

    @Test
    public void testModeratePosts_marksPostsAsVerified_whenContentIsClean() {
        List<Post> posts = new ArrayList<>();
        Post post = new Post();
        ReflectionTestUtils.setField(postService, "threadSize", 4);
        post.setContent("Clean content");
        posts.add(post);

        when(postRepository.findByVerifiedDateIsNull()).thenReturn(posts);

        lenient().when(moderationDictionaryValidation.containsBadWord(anyString())).thenReturn(false);
        lenient().when(aiModerationService.isToxic(anyString())).thenReturn(false);

        when(asyncModerationService.moderateThreadAsync(anyList()))
                .thenAnswer(invocation -> {
                    List<Post> moderatedPosts = invocation.getArgument(0);
                    moderatedPosts.forEach(p -> {
                        p.setVerifiedDate(LocalDateTime.now());
                        p.setVerified(true);
                    });
                    postRepository.saveAll(moderatedPosts);
                    return CompletableFuture.completedFuture(null);
                });

        postService.moderatePosts();

        verify(postRepository).saveAll(anyList());
    }

    @Test
    @Order(18)
    public void getUsersForBanWithUnverifiedPosts_Valid() {
        List<Long> mockUserIds = List.of(1L, 2L, 3L);
        when(postRepository.findUserIdsToBanWithUnverifiedPosts(5)).thenReturn(mockUserIds);

        List<Long> result = postService.getUsersForBanWithUnverifiedPosts(5);

        verify(postRepository, times(1)).findUserIdsToBanWithUnverifiedPosts(5);
        assertEquals(mockUserIds, result);
    }

    @Test
    void publishScheduledPostsTest() {
        when(postRepository.findReadyToPublish()).thenReturn(postsToPublish);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.publishScheduledPosts();

        verify(postRepository).findReadyToPublish();
        verify(postRepository, times(postsToPublish.size())).save(any(Post.class));
        verify(kafkaPostProducer, times(postsToPublish.size())).publishPostCreationEvent(any(Post.class));
        verify(postCacheService, times(postsToPublish.size())).cachePost(any(Post.class));
        verify(userCashService, times(postsToPublish.size())).cacheUser(anyLong());
    }

    @Test
    void testCorrectPosts() {
        Page<Post> page = new PageImpl<>(unpublishedPosts);
        when(postRepository.findByPublishedFalse(any(Pageable.class))).thenReturn(page);

        when(spellCheckerService.calculateBatchSize()).thenReturn(2);
        when(spellCheckerService.sendBatchRequestToYandexSpeller(contents))
                .thenReturn(correctedContents);

        postService.correctPosts();

        verify(spellCheckerService, times(1)).sendBatchRequestToYandexSpeller(contents);

        for (int i = 0; i < unpublishedPosts.size(); i++) {
            Post post = unpublishedPosts.get(i);
            String correctedContent = correctedContents.get(i);
            post.setContent(correctedContent);
            verify(postRepository, times(1)).saveAll(unpublishedPosts);
            assert post.getContent().equals(correctedContent);
        }
    }

    @Test
    void testCorrectPosts_EmptyPage() {
        Page<Post> emptyPage = new PageImpl<>(List.of());
        when(spellCheckerService.calculateBatchSize()).thenReturn(2);
        when(postRepository.findByPublishedFalse(any(Pageable.class))).thenReturn(emptyPage);

        postService.correctPosts();

        verify(spellCheckerService, never()).sendBatchRequestToYandexSpeller(any());

        verify(postRepository, never()).saveAll(any());
    }

    @Test
    void testCorrectPosts_ExceptionHandling() {
        Page<Post> page = new PageImpl<>(unpublishedPosts);
        when(postRepository.findByPublishedFalse(any(Pageable.class))).thenReturn(page);

        when(spellCheckerService.calculateBatchSize()).thenReturn(10);
        when(spellCheckerService.sendBatchRequestToYandexSpeller(contents))
                .thenThrow(new RuntimeException("Spell check failed"));

        postService.correctPosts();

        verify(spellCheckerService, times(1)).sendBatchRequestToYandexSpeller(contents);

        verify(postRepository, never()).saveAll(any());
    }

    @Test
    void testMethod() {
        Page<Post> page = new PageImpl<>(unpublishedPosts);
        when(postRepository.findByPublishedFalse(any(Pageable.class))).thenReturn(page);

        when(spellCheckerService.calculateBatchSize()).thenReturn(2);
        when(spellCheckerService.sendBatchRequestToYandexSpeller(contents))
                .thenReturn(correctedContents);

        postService.correctPosts();

        verify(spellCheckerService, times(1)).sendBatchRequestToYandexSpeller(contents);

        for (int i = 0; i < unpublishedPosts.size(); i++) {
            Post post = unpublishedPosts.get(i);
            String correctedContent = correctedContents.get(i);
            post.setContent(correctedContent);
            verify(postRepository, times(1)).saveAll(unpublishedPosts);
            assert post.getContent().equals(correctedContent);
        }
    }

    @Test
    void createDraft_whenProjectPostAndValidOwner_succeeds() {
        // Arrange: project post with valid project and owner
        Post projPost = new Post();
        projPost.setProjectId(1L);
        when(internalServices.projectExists(1L)).thenReturn(true);
        when(projectServiceClient.getProject(1L)).thenReturn(new faang.school.postservice.dto.project.ProjectDto(1L, "proj", 1L));
        when(postRepository.save(any(Post.class))).thenReturn(projPost);

        // Act
        Post result = postService.createDraft(projPost, 1L);

        // Assert
        assertNotNull(result);
    }

    @Test
    void createDraft_whenProjectPostAndWrongOwner_throws() {
        // Arrange: project post but user is not the project owner
        Post projPost = new Post();
        projPost.setProjectId(1L);
        when(internalServices.projectExists(1L)).thenReturn(true);
        when(projectServiceClient.getProject(1L)).thenReturn(new faang.school.postservice.dto.project.ProjectDto(1L, "proj", 99L));

        // Act & Assert
        assertThrows(DataValidationException.class, () -> postService.createDraft(projPost, 1L));
    }

    @Test
    void publish_whenPostHasNoOwner_throws() {
        // Arrange: post with neither authorId nor projectId
        Post orphanPost = new Post();
        orphanPost.setId(5L);
        orphanPost.setPublished(false);
        when(postRepository.findById(5L)).thenReturn(Optional.of(orphanPost));

        // Act & Assert
        assertThrows(DataValidationException.class, () -> postService.publish(5L, 1L));
    }

    @Test
    void moderatePosts_whenNoUnverifiedPosts_doesNothing() {
        // Arrange
        when(postRepository.findByVerifiedDateIsNull()).thenReturn(List.of());

        // Act
        postService.moderatePosts();

        // Assert
        verify(asyncModerationService, never()).moderateThreadAsync(anyList());
    }

    @Test
    void removeResources_whenOwner_removesMatchingKeys() {
        // Arrange
        Post postWithResources = new Post();
        postWithResources.setId(1L);
        postWithResources.setAuthorId(1L);
        Resource r1 = Resource.builder().key("key1").build();
        Resource r2 = Resource.builder().key("key2").build();
        postWithResources.getResources().addAll(List.of(r1, r2));

        when(postRepository.findPostsByResourceKeys(List.of("key1"))).thenReturn(List.of(postWithResources));
        when(postRepository.save(any(Post.class))).thenReturn(postWithResources);

        // Act
        postService.removeResources(List.of("key1"), 1L);

        // Assert
        assertEquals(1, postWithResources.getResources().size());
        assertEquals("key2", postWithResources.getResources().get(0).getKey());
    }
}

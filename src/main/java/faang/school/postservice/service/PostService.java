package faang.school.postservice.service;

import faang.school.postservice.client.ProjectServiceClient;
import faang.school.postservice.dto.project.ProjectDto;
import faang.school.postservice.exception.DataValidationException;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.Resource;
import faang.school.postservice.publisher.post.PostViewPublisher;
import faang.school.postservice.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final InternalServices internalServices;
    private final AsyncModerationService asyncModerationService;
    private final SpellCheckerService spellCheckerService;
    private final KafkaPostProducer kafkaPostProducer;
    private final PostCacheService postCacheService;
    private final UserCashService userCashService;
    private final PostViewPublisher postViewPublisher;
    private final ProjectServiceClient projectServiceClient;

    @Value("${moderation.threadSize}")
    private int threadSize;

    @Value("${post.query.max-size:100}")
    private int maxQuerySize;

    @Transactional
    public Post createDraft(Post post, Long currentUserId) {
        if (post.getProjectId() == null) {
            post.setAuthorId(currentUserId);
            if (!internalServices.userExists(currentUserId)) {
                throw new InvalidParameterException("Post author does not exist! id:" + currentUserId);
            }
        } else {
            post.setAuthorId(null);
            if (!internalServices.projectExists(post.getProjectId())) {
                throw new InvalidParameterException("Post project does not exist! id:" + post.getProjectId());
            }
            validateOwner(post, currentUserId);
        }
        return postRepository.save(post);
    }

    @Transactional
    public Post publish(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DataValidationException("Specified post not found. Id:" + postId));
        validateOwner(post, currentUserId);
        if (post.isPublished()) {
            throw new DataValidationException("Post is already published. Id:" + postId);
        }

        return publishPostInternal(post);
    }

    private Post publishPostInternal(Post post) {
        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        Post result = postRepository.save(post);

        TransactionHooks.runAfterCommit(() -> kafkaPostProducer.publishPostCreationEvent(result));
        TransactionHooks.runAfterCommit(() -> postCacheService.cachePost(result));
        TransactionHooks.runAfterCommit(() -> userCashService.cacheUser(result.getAuthorId()));

        return result;
    }

    @Transactional
    public Post update(Post post, Long currentUserId) {
        Post originalPost = postRepository.findById(post.getId())
                .orElseThrow(() -> new DataValidationException("You are trying to update not existing post. Id:"
                        + post.getId()));

        validateOwner(originalPost, currentUserId);

        originalPost.setContent(post.getContent());
        originalPost.setScheduledAt(post.getScheduledAt());

        Post updatedPost = postRepository.save(originalPost);
        TransactionHooks.runAfterCommit(() -> postCacheService.cachePost(updatedPost));
        return updatedPost;
    }

    @Transactional
    public void delete(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DataValidationException("Specified post not found. Id:" + postId));

        validateOwner(post, currentUserId);

        post.setDeleted(true);
        postRepository.save(post);
        TransactionHooks.runAfterCommit(() -> postCacheService.removePostFromCache(postId));
    }

    @Transactional(readOnly = true)
    public Post get(Long postId) {
        Optional<Post> cachedPost = postCacheService.getCachedPost(postId);

        Post post = cachedPost.orElseGet(() -> {
            Post storedPost = postRepository.findById(postId)
                    .orElseThrow(() -> new DataValidationException("Specified post not found. Id:" + postId));
            postCacheService.cachePost(storedPost);
            return storedPost;
        });
        TransactionHooks.runAfterCommit(() -> postViewPublisher.publishEvent(post));
        return post;

    }

    public List<Post> getDraftsByAuthorId(Long userId) {
        Pageable pageable = PageRequest.of(0, maxQuerySize);
        return postRepository.findByAuthorIdAndDeletedFalseAndPublishedFalseOrderByCreatedAtDesc(userId, pageable)
                .getContent();
    }

    public List<Post> getDraftsByProjectId(Long projectId) {
        Pageable pageable = PageRequest.of(0, maxQuerySize);
        return postRepository.findByProjectIdAndDeletedFalseAndPublishedFalseOrderByCreatedAtDesc(projectId, pageable)
                .getContent();
    }

    public List<Post> getPostsByAuthorId(Long userId) {
        Pageable pageable = PageRequest.of(0, maxQuerySize);
        return postRepository.findByAuthorIdAndDeletedFalseAndPublishedTrueOrderByPublishedAtDesc(userId, pageable)
                .getContent();
    }

    public List<Post> getPostsByProjectId(Long projectId) {
        Pageable pageable = PageRequest.of(0, maxQuerySize);
        return postRepository.findByProjectIdAndDeletedFalseAndPublishedTrueOrderByPublishedAtDesc(projectId, pageable)
                .getContent();
    }

    @Transactional(readOnly = true)
    public void moderatePosts() {
        List<Post> posts = postRepository.findByVerifiedDateIsNull();

        if (posts == null || posts.isEmpty()) {
            return;
        }

        List<List<Post>> threads = splitIntoThreads(posts);

        List<CompletableFuture<Void>> futures = threads.stream()
                .map(asyncModerationService::moderateThreadAsync)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private List<List<Post>> splitIntoThreads(List<Post> posts) {
        return ListUtils.partition(posts, threadSize);
    }

    public List<Post> findPostsByResourceKeys(List<String> resourceKeys) {
        return postRepository.findPostsByResourceKeys(resourceKeys);
    }

    @Transactional
    public Post addResources(Long postId, List<Resource> resources, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new DataValidationException("Specified post not found. Id:" + postId));
        validateOwner(post, currentUserId);
        resources.forEach(resource -> resource.setPost(post));
        post.getResources().addAll(resources);
        Post savedPost = postRepository.save(post);
        TransactionHooks.runAfterCommit(() -> postCacheService.cachePost(savedPost));
        return savedPost;
    }

    @Transactional
    public void removeResources(List<String> resourceKeys, Long currentUserId) {
        List<Post> posts = postRepository.findPostsByResourceKeys(resourceKeys);
        posts.forEach(post -> {
            validateOwner(post, currentUserId);
            post.getResources().removeIf(resource -> resourceKeys.contains(resource.getKey()));
            postRepository.save(post);
            TransactionHooks.runAfterCommit(() -> postCacheService.cachePost(post));
        });
    }

    public List<Long> getUsersForBanWithUnverifiedPosts(int maxUnverifiedPosts) {
        return postRepository.findUserIdsToBanWithUnverifiedPosts(maxUnverifiedPosts);
    }

    @Transactional
    public void publishScheduledPosts() {
        List<Post> postsToPublish = postRepository.findReadyToPublish();
        postsToPublish.forEach(this::publishPostInternal);
    }

    @Transactional
    public void correctPosts() {
        int page = 0;
        int batchSize = spellCheckerService.calculateBatchSize();
        Pageable pageable = PageRequest.of(page, batchSize);

        do {
            Page<Post> postsPage = postRepository.findByPublishedFalse(pageable);

            if (postsPage == null || postsPage.isEmpty()) {
                log.error("Received null page from repository");
                break;
            }

            List<Post> posts = postsPage.getContent();

            if (posts.isEmpty()) {
                log.info("No more posts to process");
                break;
            }

            List<String> contents = posts.stream()
                    .map(Post::getContent)
                    .collect(Collectors.toList());

            try {
                List<String> correctedContents = spellCheckerService.sendBatchRequestToYandexSpeller(contents);

                if (correctedContents.size() != contents.size()) {
                    log.error("Size mismatch: correctedContents size = {}, contents size = {}",
                            correctedContents.size(), contents.size());
                    break;
                }

                for (int i = 0; i < posts.size(); i++) {
                    Post post = posts.get(i);
                    String correctedContent = correctedContents.get(i);
                    post.setContent(correctedContent);
                }

                postRepository.saveAll(posts);
            } catch (Exception ex) {
                log.error("Failed to process batch", ex);
            }

            pageable = postsPage.nextPageable();
        } while (pageable.isPaged());
    }

    private void validateOwner(Post post, Long currentUserId) {
        if (post.getAuthorId() != null) {
            if (!Objects.equals(post.getAuthorId(), currentUserId)) {
                throw new DataValidationException("Only post owner can modify the post. Id:" + post.getId());
            }
            return;
        }

        if (post.getProjectId() != null) {
            ProjectDto project = projectServiceClient.getProject(post.getProjectId());
            if (!Objects.equals(project.ownerId(), currentUserId)) {
                throw new DataValidationException("Only project owner can modify the post. Id:" + post.getId());
            }
            return;
        }

        throw new DataValidationException("Post has no owner. Id:" + post.getId());
    }
}

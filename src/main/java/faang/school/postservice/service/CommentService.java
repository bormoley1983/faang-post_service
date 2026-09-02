package faang.school.postservice.service;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.exception.CommentNotFoundException;
import faang.school.postservice.exception.EventSerializationException;
import faang.school.postservice.exception.UserNotFoundException;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.Resource;
import faang.school.postservice.publisher.comment.AnalyticsCommentEventPublisher;
import faang.school.postservice.publisher.comment.NotificationCommentEventPublisher;
import faang.school.postservice.repository.CommentRepository;

import faang.school.postservice.service.s3.AwsService;
import faang.school.postservice.util.ModerationDictionaryUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;

    private final PostService postService;
    private final UserServiceClient userServiceClient;
    private final AwsService awsService;
    private final ResourceService resourceService;

    private final CommentValidator commentValidator;
    private final ImageProcessor imageProcessor;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${comment.image.largeImageMaxSize}")
    private int LARGE_IMAGE_MAX_SIZE;

    @Value("${comment.image.smallImageMaxSize}")
    private int SMALL_IMAGE_MAX_SIZE;

    @Value("${commenter-banner.comments-count-for-ban}")
    private int unverifiedCommentsCountForBan;

    @Value("${moderation.comment.batch-size:200}")
    private int moderationBatchSize = 200;

    @Value("${comment.query.max-size:100}")
    private int maxQuerySize = 100;

    private final ModerationDictionaryUtil moderationDictionaryUtil;
    private final AnalyticsCommentEventPublisher analyticsCommentEventPublisher;
    private final NotificationCommentEventPublisher notificationCommentEventPublisher;

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(Long postId) {
        postService.get(postId);

        return commentRepository.findAllByPostIdOrderByCreatedAtAsc(
                postId, PageRequest.of(0, maxQuerySize));
    }

    @Transactional
    public Comment createComment(Comment comment, Long postId, Long authorId) {
        Post post = postService.get(postId);
        try {
            userServiceClient.getUser(authorId);
        } catch (FeignException e) {
            throw new UserNotFoundException("User with id = " + authorId + " was not found");
        }

        comment.setPost(post);
        comment.setAuthorId(authorId);

        Comment savedComment = commentRepository.save(comment);
        TransactionHooks.runAfterCommit(() -> analyticsCommentEventPublisher.publishEvent(savedComment));
        TransactionHooks.runAfterCommit(() -> notificationCommentEventPublisher.publishEvent(savedComment));

        return savedComment;
    }

    @Transactional
    public Comment updateComment(Long commentId, Comment updatedComment, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("There is no comment with id; " + commentId));

        commentValidator.validateAuthor(comment, userId);
        commentValidator.validateCommentUpdate(updatedComment);

        String updatedContent = updatedComment.getContent();
        comment.setContent(updatedContent);

        return commentRepository.save(comment);
    }

    @Transactional
    public Comment deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("There is no comment with id + " + commentId));

        commentValidator.validateAuthor(comment, userId);
        String smallImageKey = comment.getSmallImageFileKey();
        String largeImageKey = comment.getLargeImageFileKey();

        commentRepository.delete(comment);
        TransactionHooks.runAfterCommit(() -> deleteCommentResources(smallImageKey, largeImageKey));

        return comment;
    }

    @Transactional
    public Comment attachImageToComment(Long commentId, MultipartFile image, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("There is no comment with id: " + commentId));

        commentValidator.validateAuthor(comment, userId);
        commentValidator.validateImageSize(image);
        commentValidator.validateImageFormat(image);

        String oldSmallImageKey = comment.getSmallImageFileKey();
        String oldLargeImageKey = comment.getLargeImageFileKey();

        String fileName = image.getContentType();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Image content type is required");
        }
        String format = fileName.substring(fileName.lastIndexOf('/') + 1);

        String keyLarge = generateImageKey(comment, format, "large");
        String keySmall = generateImageKey(comment, format, "small");

        try {
            long largeSize = resizeUploadImage(image, bucketName, keyLarge, format, LARGE_IMAGE_MAX_SIZE);
            long smallSize = resizeUploadImage(image, bucketName, keySmall, format, SMALL_IMAGE_MAX_SIZE);

            Resource large = Resource.builder()
                .key(keyLarge)
                .type(format)
                .size(largeSize)
                .name(fileName)
                .post(comment.getPost())
                .build();

            Resource small = Resource.builder()
                .key(keySmall)
                .type(format)
                .size(smallSize)
                .name(fileName)
                .post(comment.getPost())
                .build();

            resourceService.createResource(large);
            resourceService.createResource(small);

            comment.setLargeImageFileKey(keyLarge);
            comment.setSmallImageFileKey(keySmall);
            Comment savedComment = commentRepository.save(comment);

            TransactionHooks.runAfterCommit(() -> deleteCommentResources(oldSmallImageKey, oldLargeImageKey));
            TransactionHooks.runAfterRollback(() -> deleteCommentObjects(keySmall, keyLarge));
            return savedComment;
        } catch (RuntimeException ex) {
            deleteCommentObjects(keySmall, keyLarge);
            throw ex;
        }
    }

    @Transactional
    public List<Long> findAuthorIdsForBan() {
        return commentRepository.findAuthorsForBanWithUnverifiedCommentsCount(unverifiedCommentsCountForBan);
    }

    public byte[] getCommentImage(@PathVariable Long commentId, Function<Comment, String> keyExtractor) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("There is no comment with id " + commentId));

        String key = keyExtractor.apply(comment);

        return awsService.downloadFile(bucketName, key);
    }


    @Transactional
    public Comment deleteCommentImage(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("There is no comment with id: " + commentId));

        commentValidator.validateAuthor(comment, userId);

        String smallImageKey = comment.getSmallImageFileKey();
        String largeImageKey = comment.getLargeImageFileKey();
        comment.setSmallImageFileKey(null);
        comment.setLargeImageFileKey(null);

        Comment savedComment = commentRepository.save(comment);
        TransactionHooks.runAfterCommit(() -> deleteCommentResources(smallImageKey, largeImageKey));
        return savedComment;
    }

    private long resizeUploadImage(MultipartFile image, String bucketName, String key, String format, int maxSize) {
        try {
            BufferedImage resized = imageProcessor.resizeImage(image, maxSize);
            byte[] imageBytes = imageProcessor.bufferedImageToByteArray(resized, format);
            awsService.uploadFile(bucketName, key, imageBytes);
            return imageBytes.length;
        } catch (IOException e) {
            throw new EventSerializationException("Failed to resize and upload image", e);
        }
    }

    private String generateImageKey(Comment comment, String format, String size) {
        String timestamp = LocalDateTime.now().toString();
        return String.format("/posts/%d/Img_%s_%s.%s",
                comment.getPost().getId(), timestamp, size, format);
    }

    private void deleteCommentResources(String smallImageKey, String largeImageKey) {
        Stream.of(smallImageKey, largeImageKey)
                .filter(Objects::nonNull)
                .forEach(key -> {
                    resourceService.deleteResourceByKey(key);
                    awsService.deleteFile(bucketName, key);
                });
    }

    private void deleteCommentObjects(String smallImageKey, String largeImageKey) {
        Stream.of(smallImageKey, largeImageKey)
                .filter(Objects::nonNull)
                .forEach(key -> awsService.deleteFile(bucketName, key));
    }

    public int moderateComments() {
        int moderatedCount = 0;
        while (true) {
            Pageable pageable = PageRequest.of(0, moderationBatchSize);
            List<Long> ids = commentRepository.findUnverifiedCommentIds(pageable);

            if (ids.isEmpty()) {
                break;
            }

            List<Comment> comments = commentRepository.findAllByIdIn(ids);
            comments.forEach(comment -> {
                boolean containsBannedWords = moderationDictionaryUtil.containsBannedWords(comment.getContent());
                comment.setVerified(!containsBannedWords);
                comment.setVerifiedDate(LocalDateTime.now());
            });

            commentRepository.saveAll(comments);
            moderatedCount += comments.size();

            if (ids.size() < moderationBatchSize) {
                break;
            }
        }

        if (moderatedCount == 0) {
            log.info("No unverified comments to moderate");
            return 0;
        }

        log.info("Moderated {} comments", moderatedCount);
        return moderatedCount;
    }
}

package faang.school.postservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faang.school.postservice.model.Post;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.validation.ModerationDictionaryValidation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncModerationServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ModerationDictionaryValidation moderationDictionaryValidation;

    @Mock
    private AiModerationService aiModerationService;

    @InjectMocks
    private AsyncModerationService asyncModerationService;

    @Test
    void moderateThreadAsync_whenPostContainsBadWord_marksUnverifiedAndSaves() {
        // Arrange
        Post post = new Post();
        post.setContent("bad content");
        when(moderationDictionaryValidation.containsBadWord("bad content")).thenReturn(true);

        // Act
        CompletableFuture<Void> future = asyncModerationService.moderateThreadAsync(List.of(post));

        // Assert
        assertThat(future).isCompleted();
        assertThat(post.isVerified()).isFalse();
        assertThat(post.getVerifiedDate()).isNotNull();
        verify(aiModerationService, never()).isToxic("bad content");
        verify(postRepository).saveAll(anyList());
    }

    @Test
    void moderateThreadAsync_whenPostIsToxic_marksUnverifiedAndSaves() {
        // Arrange
        Post post = new Post();
        post.setContent("sneaky content");
        when(moderationDictionaryValidation.containsBadWord("sneaky content")).thenReturn(false);
        when(aiModerationService.isToxic("sneaky content")).thenReturn(true);

        // Act
        CompletableFuture<Void> future = asyncModerationService.moderateThreadAsync(List.of(post));

        // Assert
        assertThat(future).isCompleted();
        assertThat(post.isVerified()).isFalse();
        verify(postRepository).saveAll(anyList());
    }

    @Test
    void moderateThreadAsync_whenPostIsClean_marksVerifiedAndSaves() {
        // Arrange
        Post post = new Post();
        post.setContent("clean content");
        when(moderationDictionaryValidation.containsBadWord("clean content")).thenReturn(false);
        when(aiModerationService.isToxic("clean content")).thenReturn(false);

        // Act
        CompletableFuture<Void> future = asyncModerationService.moderateThreadAsync(List.of(post));

        // Assert
        assertThat(future).isCompleted();
        assertThat(post.isVerified()).isTrue();
        verify(postRepository).saveAll(anyList());
    }

    @Test
    void moderateThreadAsync_whenAiProviderFails_treatsAsNotToxic() {
        // Arrange
        Post post = new Post();
        post.setContent("content");
        when(moderationDictionaryValidation.containsBadWord("content")).thenReturn(false);
        when(aiModerationService.isToxic("content")).thenThrow(new RuntimeException("provider down"));

        // Act & Assert
        assertThatCode(() -> asyncModerationService.moderateThreadAsync(List.of(post)))
                .doesNotThrowAnyException();
    }

    @Test
    void moderateThreadAsync_whenRepositoryFails_returnsCompletedFutureWithoutThrowing() {
        // Arrange
        Post post = new Post();
        post.setContent("content");
        when(moderationDictionaryValidation.containsBadWord("content")).thenReturn(false);
        when(aiModerationService.isToxic("content")).thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(postRepository).saveAll(anyList());

        // Act & Assert
        CompletableFuture<Void> future = asyncModerationService.moderateThreadAsync(List.of(post));
        assertThat(future).isCompleted();
    }
}

package faang.school.postservice.service;

import faang.school.postservice.model.Comment;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.util.ModerationDictionaryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceModerationTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ModerationDictionaryUtil moderationDictionaryUtil;

    @InjectMocks
    private CommentService commentService;

    @Test
    void moderateComments_shouldUpdateVerifiedStatus() {
        ReflectionTestUtils.setField(commentService, "moderationBatchSize", 200);

        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setContent("This is a clean comment.");
        comment1.setVerified(null);
        comment1.setVerifiedDate(null);

        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setContent("This contains badword.");
        comment2.setVerified(null);
        comment2.setVerifiedDate(null);

        when(commentRepository.findUnverifiedCommentIds(any())).thenReturn(List.of(1L, 2L));
        when(commentRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(comment1, comment2));
        when(moderationDictionaryUtil.containsBannedWords("This is a clean comment.")).thenReturn(false);
        when(moderationDictionaryUtil.containsBannedWords("This contains badword.")).thenReturn(true);

        int moderatedCount = commentService.moderateComments();

        assertThat(moderatedCount).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Comment>> captor = ArgumentCaptor.forClass(List.class);
        verify(commentRepository, times(1)).saveAll(captor.capture());
        List<Comment> updatedComments = captor.getValue();

        assertThat(updatedComments).hasSize(2);

        Comment updatedComment1 = updatedComments.get(0);
        assertThat(updatedComment1.getId()).isEqualTo(1L);
        assertThat(updatedComment1.getVerified()).isTrue();
        assertThat(updatedComment1.getVerifiedDate()).isNotNull();

        Comment updatedComment2 = updatedComments.get(1);
        assertThat(updatedComment2.getId()).isEqualTo(2L);
        assertThat(updatedComment2.getVerified()).isFalse();
        assertThat(updatedComment2.getVerifiedDate()).isNotNull();
    }

    @Test
    void moderateComments_shouldHandleEmptyList() {
        ReflectionTestUtils.setField(commentService, "moderationBatchSize", 200);
        when(commentRepository.findUnverifiedCommentIds(any())).thenReturn(List.of());

        int moderatedCount = commentService.moderateComments();

        assertThat(moderatedCount).isEqualTo(0);
        verify(commentRepository, never()).saveAll(any());
    }

    @Test
    void moderateComments_shouldHandleNullContentGracefully() {
        ReflectionTestUtils.setField(commentService, "moderationBatchSize", 200);

        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setContent(null);
        comment1.setVerified(null);
        comment1.setVerifiedDate(null);

        when(commentRepository.findUnverifiedCommentIds(any())).thenReturn(List.of(1L));
        when(commentRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(comment1));
        when(moderationDictionaryUtil.containsBannedWords(null)).thenReturn(false);

        int moderatedCount = commentService.moderateComments();

        assertThat(moderatedCount).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Comment>> captor = ArgumentCaptor.forClass(List.class);
        verify(commentRepository, times(1)).saveAll(captor.capture());
        List<Comment> updatedComments = captor.getValue();

        assertThat(updatedComments).hasSize(1);

        Comment updatedComment1 = updatedComments.get(0);
        assertThat(updatedComment1.getId()).isEqualTo(1L);
        assertThat(updatedComment1.getVerified()).isTrue();
        assertThat(updatedComment1.getVerifiedDate()).isNotNull();
    }

    @Test
    void moderateComments_shouldNotSkipRowsWhenProcessedSetShrinks() {
        ReflectionTestUtils.setField(commentService, "moderationBatchSize", 2);

        Comment first = new Comment();
        first.setId(1L);
        first.setContent("first");
        Comment second = new Comment();
        second.setId(2L);
        second.setContent("second");
        Comment third = new Comment();
        third.setId(3L);
        third.setContent("third");

        when(commentRepository.findUnverifiedCommentIds(any()))
                .thenReturn(List.of(1L, 2L), List.of(3L));
        when(commentRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(commentRepository.findAllByIdIn(List.of(3L))).thenReturn(List.of(third));

        assertThat(commentService.moderateComments()).isEqualTo(3);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository, times(2)).findUnverifiedCommentIds(pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allSatisfy(pageable -> assertThat(pageable.getPageNumber()).isZero());
        verify(commentRepository, times(2)).saveAll(any());
    }
}

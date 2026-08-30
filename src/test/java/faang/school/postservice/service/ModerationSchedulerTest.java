package faang.school.postservice.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModerationSchedulerTest {

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @InjectMocks
    private ModerationScheduler moderationScheduler;

    @Test
    void runModeration_moderatesCommentsThenPosts() {
        // Arrange
        when(commentService.moderateComments()).thenReturn(5);

        // Act
        moderationScheduler.runModeration();

        // Assert
        InOrder inOrder = inOrder(commentService, postService);
        inOrder.verify(commentService).moderateComments();
        inOrder.verify(postService).moderatePosts();
    }
}

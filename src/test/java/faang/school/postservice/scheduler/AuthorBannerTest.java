package faang.school.postservice.scheduler;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import faang.school.postservice.model.event.UserBanEvent;
import faang.school.postservice.service.BanProducer;
import faang.school.postservice.service.PostService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthorBannerTest {

    @Mock
    private PostService postService;

    @Mock
    private BanProducer producer;

    @InjectMocks
    private AuthorBanner authorBanner;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authorBanner, "userBanTopic", "test-user-ban-topic");
        ReflectionTestUtils.setField(authorBanner, "unverifiedPostsCountForBan", 3);
    }

    @Test
    void banUsersWithUnverifiedPosts_whenUsersFound_sendsBanEventPerUser() {
        // Arrange
        List<Long> banUsers = List.of(1L, 2L, 3L);
        org.mockito.Mockito.when(postService.getUsersForBanWithUnverifiedPosts(3)).thenReturn(banUsers);

        // Act
        authorBanner.banUsersWithUnverifiedPosts();

        // Assert
        InOrder inOrder = inOrder(producer);
        inOrder.verify(producer).sendUserToBan("test-user-ban-topic", new UserBanEvent(1L, true));
        inOrder.verify(producer).sendUserToBan("test-user-ban-topic", new UserBanEvent(2L, true));
        inOrder.verify(producer).sendUserToBan("test-user-ban-topic", new UserBanEvent(3L, true));
    }

    @Test
    void banUsersWithUnverifiedPosts_whenNoUsersFound_doesNotSendEvents() {
        // Arrange
        org.mockito.Mockito.when(postService.getUsersForBanWithUnverifiedPosts(3)).thenReturn(List.of());

        // Act
        authorBanner.banUsersWithUnverifiedPosts();

        // Assert
        verify(producer, org.mockito.Mockito.never())
                .sendUserToBan(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }
}

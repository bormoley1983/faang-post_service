package faang.school.postservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.service.LikeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class LikeControllerTest {

    @Mock
    private LikeService likeService;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private LikeController likeController;

    @Test
    void getPostLiker_whenUsersExist_returnsUserList() {
        // Arrange
        List<UserDto> users = List.of(new UserDto(1L, "user", "user@test.com"));
        when(likeService.getUsersWhoLikedPost(1L)).thenReturn(users);

        // Act
        List<UserDto> result = likeController.getPostLiker(1L);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void getCommentLiker_whenUsersExist_returnsUserList() {
        // Arrange
        List<UserDto> users = List.of(new UserDto(1L, "user", "user@test.com"));
        when(likeService.getUsersWhoLikedComment(1L)).thenReturn(users);

        // Act
        List<UserDto> result = likeController.getCommentLiker(1L);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void addLikeToPost_whenValid_returnsNoContent() {
        // Arrange
        LikeDto likeDto = new LikeDto();
        likeDto.setPostId(1L);
        when(userContext.getUserId()).thenReturn(42L);

        // Act
        ResponseEntity<Void> response = likeController.addLikeToPost(likeDto);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(likeService).addLikeToPost(1L, null, 42L);
    }

    @Test
    void removeLikeFromPost_whenValid_returnsNoContent() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);

        // Act
        ResponseEntity<Void> response = likeController.removeLikeFromPost(1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(likeService).removeLikeFromPost(1L, 42L);
    }

    @Test
    void addLikeToComment_whenValid_returnsNoContent() {
        // Arrange
        LikeDto likeDto = new LikeDto();
        likeDto.setCommentId(5L);
        when(userContext.getUserId()).thenReturn(42L);

        // Act
        ResponseEntity<Void> response = likeController.addLikeToComment(likeDto);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(likeService).addLikeToComment(5L, null, 42L);
    }

    @Test
    void removeLikeFromComment_whenValid_returnsNoContent() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);

        // Act
        ResponseEntity<Void> response = likeController.removeLikeFromComment(5L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(likeService).removeLikeFromComment(5L, 42L);
    }
}

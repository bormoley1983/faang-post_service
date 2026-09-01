package faang.school.postservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.service.CommentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private CommentController commentController;

    private Comment testComment;
    private CommentDto testDto;

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setId(1L);
        testDto = new CommentDto();
        testDto.setId(1L);
    }

    @Test
    void getCommentsByPostId_whenCommentsExist_returnsOkWithDtoList() {
        // Arrange
        List<Comment> comments = List.of(testComment);
        when(commentService.getCommentsByPostId(1L)).thenReturn(comments);
        when(commentMapper.toDtoList(comments)).thenReturn(List.of(testDto));

        // Act
        ResponseEntity<List<CommentDto>> response = commentController.getCommentsByPostId(1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void createComment_whenValid_returnsOkWithDto() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);
        when(commentMapper.toEntity(testDto)).thenReturn(testComment);
        when(commentService.createComment(testComment, 1L, 42L)).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(testDto);

        // Act
        ResponseEntity<CommentDto> response = commentController.createComment(testDto, 1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(testDto);
    }

    @Test
    void updateComment_whenValid_returnsOkWithDto() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);
        when(commentMapper.toEntity(testDto)).thenReturn(testComment);
        when(commentService.updateComment(1L, testComment, 42L)).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(testDto);

        // Act
        ResponseEntity<CommentDto> response = commentController.updateComment(testDto, 1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void deleteComment_whenValid_returnsOkWithDto() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);
        when(commentService.deleteComment(1L, 42L)).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(testDto);

        // Act
        ResponseEntity<CommentDto> response = commentController.deleteComment(1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getSmallCommentImage_whenImageExists_returnsOkWithBytes() throws Exception {
        // Arrange
        byte[] imageBytes = new byte[]{1, 2, 3};
        when(commentService.getCommentImage(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(imageBytes);

        // Act
        ResponseEntity<byte[]> response = commentController.getSmallCommentImage(1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(imageBytes);
    }

    @Test
    void getLargeCommentImage_whenImageExists_returnsOkWithBytes() throws Exception {
        // Arrange
        byte[] imageBytes = new byte[]{4, 5, 6};
        when(commentService.getCommentImage(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(imageBytes);

        // Act
        ResponseEntity<byte[]> response = commentController.getLargeCommentImage(1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void attachImageToComment_whenValid_returnsOkWithDto() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("image", "test.png", "image/png", new byte[]{1});
        when(userContext.getUserId()).thenReturn(42L);
        when(commentService.attachImageToComment(1L, file, 42L)).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(testDto);

        // Act
        ResponseEntity<CommentDto> response = commentController.attachImageToComment(1L, file);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void deleteCommentImage_whenValid_returnsOkWithDto() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);
        when(commentService.deleteCommentImage(1L, 42L)).thenReturn(testComment);
        when(commentMapper.toDto(testComment)).thenReturn(testDto);

        // Act
        ResponseEntity<CommentDto> response = commentController.deleteCommentImage(1L);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}

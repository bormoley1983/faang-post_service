package faang.school.postservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.mapper.PostMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.service.PostService;
import faang.school.postservice.validation.PostDtoValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @Mock
    private PostMapper postMapper;

    @Mock
    private PostDtoValidator postDtoValidator;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private PostController postController;

    private Post testPost;
    private PostDto testDto;

    @BeforeEach
    void setUp() {
        testPost = new Post();
        testPost.setId(1L);
        testDto = PostDto.builder().id(1L).content("test content").build();
    }

    @Test
    void createDraft_whenNoProjectId_setsAuthorIdAndCreatesDraft() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);
        when(postMapper.toEntity(testDto)).thenReturn(testPost);
        when(postService.createDraft(testPost, 42L)).thenReturn(testPost);
        when(postMapper.toDto(testPost)).thenReturn(testDto);

        // Act
        PostDto result = postController.createDraft(testDto);

        // Assert
        assertThat(result).isSameAs(testDto);
        verify(postService).createDraft(testPost, 42L);
    }

    @Test
    void createDraft_whenProjectIdPresent_setsAuthorIdToNull() {
        // Arrange
        testDto.setProjectId(99L);
        when(userContext.getUserId()).thenReturn(42L);
        when(postMapper.toEntity(testDto)).thenReturn(testPost);
        when(postService.createDraft(testPost, 42L)).thenReturn(testPost);
        when(postMapper.toDto(testPost)).thenReturn(testDto);

        // Act
        postController.createDraft(testDto);

        // Assert
        assertThat(testDto.getAuthorId()).isNull();
    }

    @Test
    void publish_whenPostExists_returnsPublishedDto() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);
        when(postService.publish(1L, 42L)).thenReturn(testPost);
        when(postMapper.toDto(testPost)).thenReturn(testDto);

        // Act
        PostDto result = postController.publish(1L);

        // Assert
        assertThat(result).isSameAs(testDto);
    }

    @Test
    void update_whenPostExists_returnsUpdatedDto() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);
        when(postMapper.toEntity(testDto)).thenReturn(testPost);
        when(postService.update(testPost, 42L)).thenReturn(testPost);
        when(postMapper.toDto(testPost)).thenReturn(testDto);

        // Act
        PostDto result = postController.update(testDto);

        // Assert
        assertThat(result).isSameAs(testDto);
    }

    @Test
    void delete_whenPostExists_delegatesToService() {
        // Arrange
        when(userContext.getUserId()).thenReturn(42L);

        // Act
        postController.delete(1L);

        // Assert
        verify(postService).delete(1L, 42L);
    }

    @Test
    void get_whenPostExists_returnsDto() {
        // Arrange
        when(postService.get(1L)).thenReturn(testPost);
        when(postMapper.toDto(testPost)).thenReturn(testDto);

        // Act
        PostDto result = postController.get(1L);

        // Assert
        assertThat(result).isSameAs(testDto);
    }

    @Test
    void getDraftsByAuthorId_whenPostsExist_returnsDtoList() {
        // Arrange
        List<Post> posts = List.of(testPost);
        when(postService.getDraftsByAuthorId(42L)).thenReturn(posts);
        when(postMapper.toDto(posts)).thenReturn(List.of(testDto));

        // Act
        List<PostDto> result = postController.getDraftsByAuthorId(42L);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void getDraftsByProjectId_whenPostsExist_returnsDtoList() {
        // Arrange
        List<Post> posts = List.of(testPost);
        when(postService.getDraftsByProjectId(99L)).thenReturn(posts);
        when(postMapper.toDto(posts)).thenReturn(List.of(testDto));

        // Act
        List<PostDto> result = postController.getDraftsByProjectId(99L);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void getPostsByAuthorId_whenPostsExist_returnsDtoList() {
        // Arrange
        List<Post> posts = List.of(testPost);
        when(postService.getPostsByAuthorId(42L)).thenReturn(posts);
        when(postMapper.toDto(posts)).thenReturn(List.of(testDto));

        // Act
        List<PostDto> result = postController.getPostsByAuthorId(42L);

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void getPostsByProjectId_whenPostsExist_returnsDtoList() {
        // Arrange
        List<Post> posts = List.of(testPost);
        when(postService.getPostsByProjectId(99L)).thenReturn(posts);
        when(postMapper.toDto(posts)).thenReturn(List.of(testDto));

        // Act
        List<PostDto> result = postController.getPostsByProjectId(99L);

        // Assert
        assertThat(result).hasSize(1);
    }
}

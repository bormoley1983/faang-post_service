package faang.school.postservice.correcter;

import static org.mockito.Mockito.verify;

import faang.school.postservice.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCorrecterTest {

    @Mock
    private PostService postService;

    private PostCorrecter postCorrecter;

    @BeforeEach
    void setUp() {
        postCorrecter = new PostCorrecter(postService);
    }

    @Test
    void correctPostJob_delegatesToPostService() {
        // Act
        postCorrecter.correctPostJob();

        // Assert
        verify(postService).correctPosts();
    }
}

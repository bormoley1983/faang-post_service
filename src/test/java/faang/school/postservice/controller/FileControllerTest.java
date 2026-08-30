package faang.school.postservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.service.FileService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private FileController fileController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileController, "maxFiles", 10);
    }

    @Test
    void addFiles_whenWithinLimit_delegatesToService() {
        // Arrange
        List<MultipartFile> files = List.of(new MockMultipartFile("f1", "a.png", "image/png", new byte[]{1}));
        when(userContext.getUserId()).thenReturn(42L);

        // Act
        fileController.addFiles(files, 1L);

        // Assert
        verify(fileService).uploadFiles(1L, files, 42L);
    }

    @Test
    void addFiles_whenExceedsLimit_throwsIllegalArgument() {
        // Arrange: set maxFiles to 1, provide 2 files
        ReflectionTestUtils.setField(fileController, "maxFiles", 1);
        List<MultipartFile> files = List.of(
                new MockMultipartFile("f1", "a.png", "image/png", new byte[]{1}),
                new MockMultipartFile("f2", "b.png", "image/png", new byte[]{2})
        );

        // Act & Assert
        assertThatThrownBy(() -> fileController.addFiles(files, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot upload more than");
    }

    @Test
    void deleteFiles_whenValid_delegatesToService() {
        // Arrange
        List<String> fileIds = List.of("file-1", "file-2");
        when(userContext.getUserId()).thenReturn(42L);

        // Act
        fileController.deleteFiles(fileIds);

        // Assert
        verify(fileService).deleteFiles(fileIds, 42L);
    }

    @Test
    void getPresignedUrl_whenFileExists_returnsOkWithUrl() {
        // Arrange
        when(fileService.getPresignedUrl("file-1")).thenReturn("https://s3.example.com/file-1");

        // Act
        ResponseEntity<String> response = fileController.getPresignedUrl("file-1");

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("https://s3.example.com/file-1");
    }

    @Test
    void getObjectBytes_whenFileExists_returnsOkWithBytes() {
        // Arrange
        byte[] bytes = new byte[]{1, 2, 3};
        when(fileService.getObjectBytes("file-1")).thenReturn(bytes);

        // Act
        ResponseEntity<byte[]> response = fileController.getObjectBytes("file-1");

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(bytes);
    }
}

package faang.school.postservice.service;

import faang.school.postservice.config.AwsS3ApiConfig;
import faang.school.postservice.service.aws.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private AwsS3ApiConfig awsS3ApiConfig;

    @Mock
    private S3Service s3Service;

    @Mock
    private PostService postService;

    @InjectMocks
    private FileService fileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "maxFileSizeMb", 5L);
    }

    @Test
    void testUploadFiles() throws IOException {
        Long postId = 1L;

        when(awsS3ApiConfig.getBucket()).thenReturn("test-bucket");

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L * 1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("test.png");
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        PutObjectResponse putObjectResponse = PutObjectResponse.builder()
                .eTag(UUID.randomUUID().toString())
                .build();

        when(s3Service.uploadFileAsync(
                eq("test-bucket"),
                anyString(),
                anyMap(),
                any(byte[].class)
        )).thenReturn(CompletableFuture.completedFuture(putObjectResponse));

        List<String> result = fileService.uploadFiles(postId, Collections.singletonList(file), 10L);

        assertNotNull(result);
        assertEquals(1, result.size());

        verify(s3Service, times(1)).uploadFileAsync(
                eq("test-bucket"),
                anyString(),
                anyMap(),
                any(byte[].class)
        );
        verify(postService, times(1)).addResources(eq(postId), any(), eq(10L));
    }

    @Test
    void testDeleteFiles() {
        List<String> fileIds = Arrays.asList("file1", "file2");
        when(awsS3ApiConfig.getBucket()).thenReturn("test-bucket");
        when(s3Service.deleteFileAsync(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        fileService.deleteFiles(fileIds, 10L);

        verify(s3Service, times(2)).deleteFileAsync(anyString(), anyString());
        verify(postService, times(1)).removeResources(fileIds, 10L);
    }

    @Test
    void testGetPresignedUrl() {
        String fileId = "file1";
        String presignedUrl = "http://example.com/presigned-url";
        when(awsS3ApiConfig.getBucket()).thenReturn("test-bucket");
        when(s3Service.createPresignedGetUrl(anyString(), anyString())).thenReturn(presignedUrl);

        String result = fileService.getPresignedUrl(fileId);

        assertEquals(presignedUrl, result);
        verify(s3Service, times(1)).createPresignedGetUrl(anyString(), anyString());
    }

    @Test
    void testGetObjectBytes() {
        String fileId = "file1";
        byte[] fileBytes = new byte[]{1, 2, 3};
        when(awsS3ApiConfig.getBucket()).thenReturn("test-bucket");
        when(s3Service.getObjectBytes(anyString(), anyString())).thenReturn(fileBytes);

        byte[] result = fileService.getObjectBytes(fileId);

        assertArrayEquals(fileBytes, result);
        verify(s3Service, times(1)).getObjectBytes(anyString(), anyString());
    }

    @Test
    void uploadFiles_whenFileTooLarge_throwsAndCleansUp() {
        // Arrange: file larger than 5MB — size check throws before contentType is read
        MultipartFile largeFile = mock(MultipartFile.class);
        when(largeFile.getSize()).thenReturn(6L * 1024 * 1024);

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                fileService.uploadFiles(1L, List.of(largeFile), 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size");
    }

    @Test
    void uploadFiles_whenUnsupportedType_throws() {
        // Arrange
        MultipartFile pdfFile = mock(MultipartFile.class);
        when(pdfFile.getSize()).thenReturn(1024L);
        when(pdfFile.getContentType()).thenReturn("application/pdf");

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                fileService.uploadFiles(1L, List.of(pdfFile), 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void uploadFiles_whenContentTypeNull_throws() {
        // Arrange
        MultipartFile noTypeFile = mock(MultipartFile.class);
        when(noTypeFile.getSize()).thenReturn(1024L);
        when(noTypeFile.getContentType()).thenReturn(null);

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                fileService.uploadFiles(1L, List.of(noTypeFile), 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void uploadFiles_whenSecondFileFails_cleansUpFirstUploaded() throws IOException {
        // Arrange: first file succeeds, second has unsupported type
        MultipartFile file1 = mock(MultipartFile.class);
        when(file1.getSize()).thenReturn(1024L);
        when(file1.getContentType()).thenReturn("video/mp4");
        when(file1.getOriginalFilename()).thenReturn("a.mp4");
        when(file1.getBytes()).thenReturn(new byte[]{1});

        MultipartFile file2 = mock(MultipartFile.class);
        when(file2.getSize()).thenReturn(1024L);
        when(file2.getContentType()).thenReturn("application/pdf");

        when(awsS3ApiConfig.getBucket()).thenReturn("test-bucket");
        when(s3Service.uploadFileAsync(eq("test-bucket"), anyString(), anyMap(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(s3Service.deleteFileAsync(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                fileService.uploadFiles(1L, List.of(file1, file2), 2L))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify cleanup was attempted for the first file's key
        verify(s3Service).deleteFileAsync(eq("test-bucket"), anyString());
    }
}

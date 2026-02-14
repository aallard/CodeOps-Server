package com.codeops.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock private S3Client s3Client;

    private S3StorageService s3StorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        s3StorageService = new S3StorageService();
        setField(s3StorageService, "localStoragePath", tempDir.toString());
        setField(s3StorageService, "bucket", "test-bucket");
    }

    // --- upload (local mode) ---

    @Test
    void upload_localMode_writesFile() {
        setField(s3StorageService, "s3Enabled", false);

        byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String key = s3StorageService.upload("test/file.txt", data, "text/plain");

        assertEquals("test/file.txt", key);
        Path filePath = tempDir.resolve("test/file.txt");
        assertTrue(Files.exists(filePath));
    }

    @Test
    void upload_localMode_createsDirectories() {
        setField(s3StorageService, "s3Enabled", false);

        byte[] data = "content".getBytes(StandardCharsets.UTF_8);
        s3StorageService.upload("deep/nested/path/file.md", data, "text/markdown");

        assertTrue(Files.exists(tempDir.resolve("deep/nested/path/file.md")));
    }

    @Test
    void upload_localMode_fileContentCorrect() throws IOException {
        setField(s3StorageService, "s3Enabled", false);

        byte[] data = "test content".getBytes(StandardCharsets.UTF_8);
        s3StorageService.upload("content-test.txt", data, "text/plain");

        byte[] written = Files.readAllBytes(tempDir.resolve("content-test.txt"));
        assertArrayEquals(data, written);
    }

    // --- upload (S3 mode) ---

    @Test
    void upload_s3Mode_callsS3Client() {
        setField(s3StorageService, "s3Enabled", true);
        setField(s3StorageService, "s3Client", s3Client);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        String key = s3StorageService.upload("reports/file.md", data, "text/markdown");

        assertEquals("reports/file.md", key);

        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(reqCaptor.capture(), any(RequestBody.class));
        assertEquals("test-bucket", reqCaptor.getValue().bucket());
        assertEquals("reports/file.md", reqCaptor.getValue().key());
        assertEquals("text/markdown", reqCaptor.getValue().contentType());
    }

    @Test
    void upload_s3EnabledButClientNull_fallsToLocal() {
        setField(s3StorageService, "s3Enabled", true);
        setField(s3StorageService, "s3Client", null);

        byte[] data = "fallback".getBytes(StandardCharsets.UTF_8);
        String key = s3StorageService.upload("fallback.txt", data, "text/plain");

        assertEquals("fallback.txt", key);
        assertTrue(Files.exists(tempDir.resolve("fallback.txt")));
    }

    // --- download (local mode) ---

    @Test
    void download_localMode_readsFile() throws IOException {
        setField(s3StorageService, "s3Enabled", false);

        Path filePath = tempDir.resolve("download-test.txt");
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, "downloaded content".getBytes(StandardCharsets.UTF_8));

        byte[] result = s3StorageService.download("download-test.txt");
        assertEquals("downloaded content", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void download_localMode_fileNotFound_throwsRuntime() {
        setField(s3StorageService, "s3Enabled", false);

        assertThrows(RuntimeException.class, () ->
                s3StorageService.download("nonexistent/file.txt"));
    }

    // --- download (S3 mode) ---

    @Test
    void download_s3Mode_callsS3Client() throws IOException {
        setField(s3StorageService, "s3Enabled", true);
        setField(s3StorageService, "s3Client", s3Client);

        byte[] s3Data = "s3 data".getBytes(StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> mockResponse =
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        new ByteArrayInputStream(s3Data));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockResponse);

        byte[] result = s3StorageService.download("reports/file.md");
        assertArrayEquals(s3Data, result);

        ArgumentCaptor<GetObjectRequest> reqCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(reqCaptor.capture());
        assertEquals("test-bucket", reqCaptor.getValue().bucket());
        assertEquals("reports/file.md", reqCaptor.getValue().key());
    }

    // --- delete (local mode) ---

    @Test
    void delete_localMode_deletesFile() throws IOException {
        setField(s3StorageService, "s3Enabled", false);

        Path filePath = tempDir.resolve("to-delete.txt");
        Files.write(filePath, "delete me".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.exists(filePath));

        s3StorageService.delete("to-delete.txt");
        assertFalse(Files.exists(filePath));
    }

    @Test
    void delete_localMode_nonexistentFile_noError() {
        setField(s3StorageService, "s3Enabled", false);

        assertDoesNotThrow(() -> s3StorageService.delete("nonexistent.txt"));
    }

    // --- delete (S3 mode) ---

    @Test
    void delete_s3Mode_callsS3Client() {
        setField(s3StorageService, "s3Enabled", true);
        setField(s3StorageService, "s3Client", s3Client);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        s3StorageService.delete("reports/old-file.md");

        ArgumentCaptor<DeleteObjectRequest> reqCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(reqCaptor.capture());
        assertEquals("test-bucket", reqCaptor.getValue().bucket());
        assertEquals("reports/old-file.md", reqCaptor.getValue().key());
    }

    // --- generatePresignedUrl ---

    @Test
    void generatePresignedUrl_s3Mode_returnsS3Uri() {
        setField(s3StorageService, "s3Enabled", true);
        setField(s3StorageService, "s3Client", s3Client);

        String url = s3StorageService.generatePresignedUrl("reports/file.md", Duration.ofHours(1));
        assertEquals("s3://test-bucket/reports/file.md", url);
    }

    @Test
    void generatePresignedUrl_localMode_returnsLocalUri() {
        setField(s3StorageService, "s3Enabled", false);

        String url = s3StorageService.generatePresignedUrl("reports/file.md", Duration.ofHours(1));
        assertEquals("local://reports/file.md", url);
    }

    // --- round-trip (local mode) ---

    @Test
    void uploadAndDownload_localMode_roundTrip() {
        setField(s3StorageService, "s3Enabled", false);

        byte[] original = "round trip data".getBytes(StandardCharsets.UTF_8);
        String key = s3StorageService.upload("round-trip/test.bin", original, "application/octet-stream");

        byte[] downloaded = s3StorageService.download(key);
        assertArrayEquals(original, downloaded);
    }

    /**
     * Helper to set private/injected fields via reflection.
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}

package com.codeops.controller;

import com.codeops.entity.enums.AgentType;
import com.codeops.service.ReportStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportStorageService reportStorageService;

    private ReportController controller;

    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ReportController(reportStorageService);
    }

    @Test
    void uploadAgentReport_returnsCreatedWithS3Key() {
        String markdown = "## Security Report";
        String s3Key = "reports/job1/security.md";
        when(reportStorageService.uploadReport(jobId, AgentType.SECURITY, markdown)).thenReturn(s3Key);

        ResponseEntity<Map<String, String>> result = controller.uploadAgentReport(jobId, AgentType.SECURITY, markdown);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).containsEntry("s3Key", s3Key);
        verify(reportStorageService).uploadReport(jobId, AgentType.SECURITY, markdown);
    }

    @Test
    void uploadSummaryReport_returnsCreatedWithS3Key() {
        String markdown = "## Summary Report";
        String s3Key = "reports/job1/summary.md";
        when(reportStorageService.uploadSummaryReport(jobId, markdown)).thenReturn(s3Key);

        ResponseEntity<Map<String, String>> result = controller.uploadSummaryReport(jobId, markdown);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).containsEntry("s3Key", s3Key);
        verify(reportStorageService).uploadSummaryReport(jobId, markdown);
    }

    @Test
    void downloadReport_returnsOkWithMarkdownContent() {
        String s3Key = "reports/job1/security.md";
        String content = "## Security Report\nFindings here.";
        when(reportStorageService.downloadReport(s3Key)).thenReturn(content);

        ResponseEntity<String> result = controller.downloadReport(s3Key);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(content);
        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("text/markdown"));
        verify(reportStorageService).downloadReport(s3Key);
    }

    @Test
    void downloadReport_rejectsPathTraversal() {
        assertThatThrownBy(() -> controller.downloadReport("../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    void downloadReport_rejectsLeadingSlash() {
        assertThatThrownBy(() -> controller.downloadReport("/etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    void downloadReport_rejectsBlankKey() {
        assertThatThrownBy(() -> controller.downloadReport(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("S3 key is required");
    }

    @Test
    void downloadReport_rejectsInvalidCharacters() {
        assertThatThrownBy(() -> controller.downloadReport("reports/file name with spaces.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid S3 key format");
    }

    @Test
    void uploadSpecification_returnsCreatedWithS3Key() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "spec.pdf",
                "application/pdf", "pdf-bytes".getBytes());
        String s3Key = "specs/job1/spec.pdf";
        when(reportStorageService.uploadSpecification(jobId, "spec.pdf", file.getBytes(), "application/pdf"))
                .thenReturn(s3Key);

        ResponseEntity<Map<String, String>> result = controller.uploadSpecification(jobId, file);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).containsEntry("s3Key", s3Key);
        verify(reportStorageService).uploadSpecification(jobId, "spec.pdf", file.getBytes(), "application/pdf");
    }

    @Test
    void uploadSpecification_rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe",
                "application/x-msdownload", "bytes".getBytes());

        assertThatThrownBy(() -> controller.uploadSpecification(jobId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void uploadSpecification_rejectsOversizedFile() {
        byte[] largeContent = new byte[51 * 1024 * 1024]; // 51 MB
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf",
                "application/pdf", largeContent);

        assertThatThrownBy(() -> controller.uploadSpecification(jobId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too large");
    }

    @Test
    void downloadSpecification_returnsOkWithBytes() {
        String s3Key = "specs/job1/spec.pdf";
        byte[] data = "pdf-content".getBytes();
        when(reportStorageService.downloadSpecification(s3Key)).thenReturn(data);

        ResponseEntity<byte[]> result = controller.downloadSpecification(s3Key);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(data);
        assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        verify(reportStorageService).downloadSpecification(s3Key);
    }

    @Test
    void downloadSpecification_rejectsPathTraversal() {
        assertThatThrownBy(() -> controller.downloadSpecification("../etc/secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("path traversal");
    }
}

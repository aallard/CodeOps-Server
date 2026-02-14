package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.AgentRun;
import com.codeops.entity.enums.AgentType;
import com.codeops.repository.AgentRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportStorageServiceTest {

    @Mock private S3StorageService s3StorageService;
    @Mock private AgentRunRepository agentRunRepository;

    @InjectMocks
    private ReportStorageService reportStorageService;

    private UUID jobId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
    }

    // --- uploadReport ---

    @Test
    void uploadReport_success() {
        String expectedKey = AppConstants.S3_REPORTS + jobId + "/security-report.md";
        String markdownContent = "# Security Report\nNo issues found.";

        when(s3StorageService.upload(eq(expectedKey), any(byte[].class), eq("text/markdown")))
                .thenReturn(expectedKey);

        String key = reportStorageService.uploadReport(jobId, AgentType.SECURITY, markdownContent);

        assertEquals(expectedKey, key);
        verify(s3StorageService).upload(
                eq(expectedKey),
                eq(markdownContent.getBytes(StandardCharsets.UTF_8)),
                eq("text/markdown")
        );
    }

    @Test
    void uploadReport_differentAgentTypes() {
        for (AgentType agentType : AgentType.values()) {
            String expectedKey = AppConstants.S3_REPORTS + jobId + "/" + agentType.name().toLowerCase() + "-report.md";
            when(s3StorageService.upload(eq(expectedKey), any(byte[].class), eq("text/markdown")))
                    .thenReturn(expectedKey);

            String key = reportStorageService.uploadReport(jobId, agentType, "content");
            assertEquals(expectedKey, key);
        }
    }

    // --- uploadSummaryReport ---

    @Test
    void uploadSummaryReport_success() {
        String expectedKey = AppConstants.S3_REPORTS + jobId + "/summary.md";
        String markdownContent = "# Summary\nOverall health: 85/100";

        when(s3StorageService.upload(eq(expectedKey), any(byte[].class), eq("text/markdown")))
                .thenReturn(expectedKey);

        String key = reportStorageService.uploadSummaryReport(jobId, markdownContent);

        assertEquals(expectedKey, key);
        verify(s3StorageService).upload(
                eq(expectedKey),
                eq(markdownContent.getBytes(StandardCharsets.UTF_8)),
                eq("text/markdown")
        );
    }

    // --- downloadReport ---

    @Test
    void downloadReport_success() {
        String s3Key = "reports/" + jobId + "/security-report.md";
        String expectedContent = "# Security Report";
        when(s3StorageService.download(s3Key))
                .thenReturn(expectedContent.getBytes(StandardCharsets.UTF_8));

        String result = reportStorageService.downloadReport(s3Key);

        assertEquals(expectedContent, result);
        verify(s3StorageService).download(s3Key);
    }

    @Test
    void downloadReport_emptyContent() {
        String s3Key = "reports/empty.md";
        when(s3StorageService.download(s3Key)).thenReturn(new byte[0]);

        String result = reportStorageService.downloadReport(s3Key);
        assertEquals("", result);
    }

    // --- deleteReportsForJob ---

    @Test
    void deleteReportsForJob_deletesAllAgentRunReports() {
        AgentRun run1 = AgentRun.builder().reportS3Key("reports/" + jobId + "/security-report.md").build();
        run1.setId(UUID.randomUUID());
        AgentRun run2 = AgentRun.builder().reportS3Key("reports/" + jobId + "/code_quality-report.md").build();
        run2.setId(UUID.randomUUID());

        when(agentRunRepository.findByJobId(jobId)).thenReturn(List.of(run1, run2));

        reportStorageService.deleteReportsForJob(jobId);

        verify(s3StorageService).delete("reports/" + jobId + "/security-report.md");
        verify(s3StorageService).delete("reports/" + jobId + "/code_quality-report.md");
        // Also deletes summary
        verify(s3StorageService).delete(AppConstants.S3_REPORTS + jobId + "/summary.md");
    }

    @Test
    void deleteReportsForJob_skipsNullReportKeys() {
        AgentRun runWithReport = AgentRun.builder().reportS3Key("reports/" + jobId + "/security-report.md").build();
        runWithReport.setId(UUID.randomUUID());
        AgentRun runWithoutReport = AgentRun.builder().reportS3Key(null).build();
        runWithoutReport.setId(UUID.randomUUID());

        when(agentRunRepository.findByJobId(jobId)).thenReturn(List.of(runWithReport, runWithoutReport));

        reportStorageService.deleteReportsForJob(jobId);

        verify(s3StorageService).delete("reports/" + jobId + "/security-report.md");
        // Summary delete still attempted
        verify(s3StorageService).delete(AppConstants.S3_REPORTS + jobId + "/summary.md");
        // Only 2 total delete calls (1 report + 1 summary), not 3
        verify(s3StorageService, times(2)).delete(any());
    }

    @Test
    void deleteReportsForJob_noAgentRuns_stillDeletesSummary() {
        when(agentRunRepository.findByJobId(jobId)).thenReturn(List.of());

        reportStorageService.deleteReportsForJob(jobId);

        verify(s3StorageService).delete(AppConstants.S3_REPORTS + jobId + "/summary.md");
        verify(s3StorageService, times(1)).delete(any());
    }

    @Test
    void deleteReportsForJob_summaryDeleteFails_doesNotThrow() {
        when(agentRunRepository.findByJobId(jobId)).thenReturn(List.of());
        doThrow(new RuntimeException("S3 error"))
                .when(s3StorageService).delete(AppConstants.S3_REPORTS + jobId + "/summary.md");

        assertDoesNotThrow(() -> reportStorageService.deleteReportsForJob(jobId));
    }

    // --- uploadSpecification ---

    @Test
    void uploadSpecification_success() {
        String fileName = "api-spec.yaml";
        byte[] fileData = "openapi: 3.0.0".getBytes(StandardCharsets.UTF_8);
        String expectedKey = AppConstants.S3_SPECS + jobId + "/" + fileName;

        when(s3StorageService.upload(eq(expectedKey), eq(fileData), eq("application/yaml")))
                .thenReturn(expectedKey);

        String key = reportStorageService.uploadSpecification(jobId, fileName, fileData, "application/yaml");

        assertEquals(expectedKey, key);
        verify(s3StorageService).upload(expectedKey, fileData, "application/yaml");
    }

    @Test
    void uploadSpecification_differentContentTypes() {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        String expectedKey = AppConstants.S3_SPECS + jobId + "/spec.pdf";

        when(s3StorageService.upload(eq(expectedKey), eq(data), eq("application/pdf")))
                .thenReturn(expectedKey);

        String key = reportStorageService.uploadSpecification(jobId, "spec.pdf", data, "application/pdf");
        assertEquals(expectedKey, key);
    }

    // --- downloadSpecification ---

    @Test
    void downloadSpecification_success() {
        String s3Key = "specs/" + jobId + "/api-spec.yaml";
        byte[] expectedData = "openapi: 3.0.0".getBytes(StandardCharsets.UTF_8);
        when(s3StorageService.download(s3Key)).thenReturn(expectedData);

        byte[] result = reportStorageService.downloadSpecification(s3Key);

        assertArrayEquals(expectedData, result);
        verify(s3StorageService).download(s3Key);
    }
}

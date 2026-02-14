package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.AgentRun;
import com.codeops.entity.enums.AgentType;
import com.codeops.repository.AgentRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportStorageService {

    private final S3StorageService s3StorageService;
    private final AgentRunRepository agentRunRepository;

    public String uploadReport(UUID jobId, AgentType agentType, String markdownContent) {
        String key = AppConstants.S3_REPORTS + jobId + "/" + agentType.name().toLowerCase() + "-report.md";
        s3StorageService.upload(key, markdownContent.getBytes(StandardCharsets.UTF_8), "text/markdown");
        return key;
    }

    public String uploadSummaryReport(UUID jobId, String markdownContent) {
        String key = AppConstants.S3_REPORTS + jobId + "/summary.md";
        s3StorageService.upload(key, markdownContent.getBytes(StandardCharsets.UTF_8), "text/markdown");
        return key;
    }

    public String downloadReport(String s3Key) {
        byte[] data = s3StorageService.download(s3Key);
        return new String(data, StandardCharsets.UTF_8);
    }

    public void deleteReportsForJob(UUID jobId) {
        List<AgentRun> runs = agentRunRepository.findByJobId(jobId);
        for (AgentRun run : runs) {
            if (run.getReportS3Key() != null) {
                s3StorageService.delete(run.getReportS3Key());
            }
        }
        String summaryKey = AppConstants.S3_REPORTS + jobId + "/summary.md";
        try {
            s3StorageService.delete(summaryKey);
        } catch (Exception ignored) {
            // Summary may not exist
        }
    }

    public String uploadSpecification(UUID jobId, String fileName, byte[] fileData, String contentType) {
        String key = AppConstants.S3_SPECS + jobId + "/" + fileName;
        s3StorageService.upload(key, fileData, contentType);
        return key;
    }

    public byte[] downloadSpecification(String s3Key) {
        return s3StorageService.download(s3Key);
    }
}

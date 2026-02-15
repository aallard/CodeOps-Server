package com.codeops.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.AgentRun;
import com.codeops.entity.enums.AgentType;
import com.codeops.repository.AgentRunRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Handles storage and retrieval of agent-generated reports and specification files.
 *
 * <p>Delegates all storage operations to {@link S3StorageService}, which transparently
 * switches between AWS S3 and local filesystem storage based on configuration. Reports
 * are stored as UTF-8 encoded markdown files organized by job ID and agent type.</p>
 *
 * @see S3StorageService
 * @see ReportController
 * @see AgentRun
 */
@Service
@RequiredArgsConstructor
public class ReportStorageService {

    private static final Logger log = LoggerFactory.getLogger(ReportStorageService.class);

    private final S3StorageService s3StorageService;
    private final AgentRunRepository agentRunRepository;

    /**
     * Uploads an agent-specific report as a markdown file.
     *
     * <p>The report is stored under the key pattern
     * {@code reports/{jobId}/{agentType}-report.md}.</p>
     *
     * @param jobId the ID of the QA job this report belongs to
     * @param agentType the type of agent that generated the report (used in the filename)
     * @param markdownContent the markdown content of the report
     * @return the S3 key where the report was stored
     */
    public String uploadReport(UUID jobId, AgentType agentType, String markdownContent) {
        log.debug("uploadReport called with jobId={}, agentType={}", jobId, agentType);
        String key = AppConstants.S3_REPORTS + jobId + "/" + agentType.name().toLowerCase() + "-report.md";
        s3StorageService.upload(key, markdownContent.getBytes(StandardCharsets.UTF_8), "text/markdown");
        log.info("Uploaded report for jobId={}, agentType={}, key={}", jobId, agentType, key);
        return key;
    }

    /**
     * Uploads a summary report for a QA job as a markdown file.
     *
     * <p>The summary report aggregates findings across all agent runs and is stored
     * under the key pattern {@code reports/{jobId}/summary.md}.</p>
     *
     * @param jobId the ID of the QA job this summary belongs to
     * @param markdownContent the markdown content of the summary report
     * @return the S3 key where the summary was stored
     */
    public String uploadSummaryReport(UUID jobId, String markdownContent) {
        log.debug("uploadSummaryReport called with jobId={}", jobId);
        String key = AppConstants.S3_REPORTS + jobId + "/summary.md";
        s3StorageService.upload(key, markdownContent.getBytes(StandardCharsets.UTF_8), "text/markdown");
        log.info("Uploaded summary report for jobId={}, key={}", jobId, key);
        return key;
    }

    /**
     * Downloads a report from storage and returns it as a UTF-8 string.
     *
     * @param s3Key the storage key of the report to download
     * @return the report content as a UTF-8 decoded string
     * @throws RuntimeException if the download fails
     */
    public String downloadReport(String s3Key) {
        log.debug("downloadReport called with s3Key={}", s3Key);
        byte[] data = s3StorageService.download(s3Key);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Deletes all reports associated with a QA job from storage.
     *
     * <p>Iterates through all agent runs for the job and deletes their individual
     * report files, then attempts to delete the summary report. If the summary
     * report does not exist, the exception is silently ignored.</p>
     *
     * @param jobId the ID of the QA job whose reports should be deleted
     */
    public void deleteReportsForJob(UUID jobId) {
        log.debug("deleteReportsForJob called with jobId={}", jobId);
        List<AgentRun> runs = agentRunRepository.findByJobId(jobId);
        for (AgentRun run : runs) {
            if (run.getReportS3Key() != null) {
                s3StorageService.delete(run.getReportS3Key());
            }
        }
        String summaryKey = AppConstants.S3_REPORTS + jobId + "/summary.md";
        try {
            s3StorageService.delete(summaryKey);
        } catch (Exception e) {
            log.warn("Summary report not found for jobId={}, skipping deletion", jobId);
        }
        log.info("Deleted reports for jobId={}, agentRunCount={}", jobId, runs.size());
    }

    /**
     * Uploads a specification file associated with a QA job.
     *
     * <p>The file is stored under the key pattern {@code specs/{jobId}/{fileName}}.</p>
     *
     * @param jobId the ID of the QA job this specification belongs to
     * @param fileName the original filename of the specification
     * @param fileData the raw byte content of the file
     * @param contentType the MIME content type of the file
     * @return the S3 key where the specification was stored
     */
    public String uploadSpecification(UUID jobId, String fileName, byte[] fileData, String contentType) {
        log.debug("uploadSpecification called with jobId={}, fileName={}, contentType={}", jobId, fileName, contentType);
        String key = AppConstants.S3_SPECS + jobId + "/" + fileName;
        s3StorageService.upload(key, fileData, contentType);
        log.info("Uploaded specification for jobId={}, key={}", jobId, key);
        return key;
    }

    /**
     * Downloads a specification file from storage as raw bytes.
     *
     * @param s3Key the storage key of the specification to download
     * @return the raw byte content of the specification file
     * @throws RuntimeException if the download fails
     */
    public byte[] downloadSpecification(String s3Key) {
        log.debug("downloadSpecification called with s3Key={}", s3Key);
        return s3StorageService.download(s3Key);
    }
}

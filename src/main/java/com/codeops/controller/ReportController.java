package com.codeops.controller;

import com.codeops.entity.enums.AgentType;
import com.codeops.service.ReportStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

    private final ReportStorageService reportStorageService;

    private static final Pattern SAFE_S3_KEY = Pattern.compile("^[a-zA-Z0-9/_\\-\\.]+$");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "text/plain", "text/markdown", "text/csv",
            "application/json", "application/xml", "text/xml",
            "image/png", "image/jpeg", "image/gif");
    private static final long MAX_UPLOAD_SIZE = 50 * 1024 * 1024; // 50 MB

    private void validateS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("S3 key is required");
        }
        if (s3Key.contains("..") || s3Key.startsWith("/")) {
            throw new IllegalArgumentException("Invalid S3 key: path traversal detected");
        }
        if (!SAFE_S3_KEY.matcher(s3Key).matches()) {
            throw new IllegalArgumentException("Invalid S3 key format");
        }
    }

    @PostMapping("/job/{jobId}/agent/{agentType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadAgentReport(
            @PathVariable UUID jobId,
            @PathVariable AgentType agentType,
            @RequestBody String markdownContent) {
        String key = reportStorageService.uploadReport(jobId, agentType, markdownContent);
        return ResponseEntity.status(201).body(Map.of("s3Key", key));
    }

    @PostMapping("/job/{jobId}/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadSummaryReport(
            @PathVariable UUID jobId,
            @RequestBody String markdownContent) {
        String key = reportStorageService.uploadSummaryReport(jobId, markdownContent);
        return ResponseEntity.status(201).body(Map.of("s3Key", key));
    }

    @GetMapping("/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> downloadReport(@RequestParam String s3Key) {
        validateS3Key(s3Key);
        String content = reportStorageService.downloadReport(s3Key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown"))
                .body(content);
    }

    @PostMapping("/job/{jobId}/spec")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadSpecification(
            @PathVariable UUID jobId,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("File too large (max 50MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type");
        }
        String filename = file.getOriginalFilename();
        if (filename != null) {
            filename = filename.replaceAll("[/\\\\]", "_"); // Strip path separators
        }
        String key = reportStorageService.uploadSpecification(
                jobId, filename, file.getBytes(), contentType);
        return ResponseEntity.status(201).body(Map.of("s3Key", key));
    }

    @GetMapping("/spec/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadSpecification(@RequestParam String s3Key) {
        validateS3Key(s3Key);
        byte[] data = reportStorageService.downloadSpecification(s3Key);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}

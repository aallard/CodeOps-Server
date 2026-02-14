package com.codeops.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Service
@Slf4j
public class S3StorageService {

    @Value("${codeops.aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${codeops.aws.s3.bucket:codeops-dev}")
    private String bucket;

    @Value("${codeops.local-storage.path:#{systemProperties['user.home']}/.codeops/storage}")
    private String localStoragePath;

    @Autowired(required = false)
    private S3Client s3Client;

    public String upload(String key, byte[] data, String contentType) {
        if (s3Enabled && s3Client != null) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(data));
            log.debug("Uploaded to S3: {}/{}", bucket, key);
        } else {
            try {
                Path filePath = Paths.get(localStoragePath, key);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, data);
                log.debug("Uploaded to local storage: {}", filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write to local storage", e);
            }
        }
        return key;
    }

    public byte[] download(String key) {
        if (s3Enabled && s3Client != null) {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            try {
                return s3Client.getObject(request).readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to download from S3", e);
            }
        } else {
            try {
                return Files.readAllBytes(Paths.get(localStoragePath, key));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read from local storage", e);
            }
        }
    }

    public void delete(String key) {
        if (s3Enabled && s3Client != null) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.debug("Deleted from S3: {}/{}", bucket, key);
        } else {
            try {
                Files.deleteIfExists(Paths.get(localStoragePath, key));
                log.debug("Deleted from local storage: {}", key);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete from local storage", e);
            }
        }
    }

    public String generatePresignedUrl(String key, Duration expiry) {
        if (s3Enabled && s3Client != null) {
            // For presigned URLs, would need S3Presigner — returning S3 URI for now
            return "s3://" + bucket + "/" + key;
        } else {
            return "local://" + key;
        }
    }
}

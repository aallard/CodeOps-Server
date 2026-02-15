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

/**
 * Provides file storage operations with automatic fallback between AWS S3 and local filesystem.
 *
 * <p>When {@code codeops.aws.s3.enabled} is {@code true} and an S3Client bean is available,
 * all operations target the configured S3 bucket. Otherwise, files are stored on the local
 * filesystem under {@code codeops.local-storage.path} (defaults to {@code ~/.codeops/storage/}).</p>
 *
 * <p>This service is used as the storage backend by {@link ReportStorageService} and
 * {@link RemediationTaskService} for persisting reports, specifications, and task prompts.</p>
 *
 * @see ReportStorageService
 */
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

    /**
     * Uploads data to S3 or local filesystem storage.
     *
     * <p>When S3 is enabled, uploads to the configured bucket. When S3 is disabled,
     * writes the data to a file under the local storage path, creating any necessary
     * parent directories.</p>
     *
     * @param key the storage key (used as the S3 object key or local file path relative to storage root)
     * @param data the raw byte content to upload
     * @param contentType the MIME content type of the data (used as S3 object metadata)
     * @return the storage key that was written to
     * @throws RuntimeException if writing to local storage fails
     */
    public String upload(String key, byte[] data, String contentType) {
        log.debug("upload called with key={}, contentType={}, dataSize={}", key, contentType, data.length);
        if (s3Enabled && s3Client != null) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(data));
            log.info("Uploaded to S3: bucket={}, key={}, size={}", bucket, key, data.length);
        } else {
            log.info("S3 disabled, using local fallback for upload key={}", key);
            try {
                Path filePath = Paths.get(localStoragePath, key);
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, data);
                log.info("Uploaded to local storage: path={}, size={}", filePath, data.length);
            } catch (IOException e) {
                log.error("Failed to write to local storage for key={}", key, e);
                throw new RuntimeException("Failed to write to local storage", e);
            }
        }
        return key;
    }

    /**
     * Downloads data from S3 or local filesystem storage.
     *
     * <p>When S3 is enabled, retrieves the object from the configured bucket.
     * When S3 is disabled, reads the file from the local storage path.</p>
     *
     * @param key the storage key of the object to download
     * @return the raw byte content of the stored object
     * @throws RuntimeException if the download or file read fails
     */
    public byte[] download(String key) {
        log.debug("download called with key={}", key);
        if (s3Enabled && s3Client != null) {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            try {
                byte[] data = s3Client.getObject(request).readAllBytes();
                log.info("Downloaded from S3: bucket={}, key={}, size={}", bucket, key, data.length);
                return data;
            } catch (IOException e) {
                log.error("Failed to download from S3 for key={}", key, e);
                throw new RuntimeException("Failed to download from S3", e);
            }
        } else {
            log.info("S3 disabled, using local fallback for download key={}", key);
            try {
                byte[] data = Files.readAllBytes(Paths.get(localStoragePath, key));
                log.info("Downloaded from local storage: key={}, size={}", key, data.length);
                return data;
            } catch (IOException e) {
                log.error("Failed to read from local storage for key={}", key, e);
                throw new RuntimeException("Failed to read from local storage", e);
            }
        }
    }

    /**
     * Deletes an object from S3 or local filesystem storage.
     *
     * <p>When S3 is enabled, deletes the object from the configured bucket.
     * When S3 is disabled, deletes the file from the local storage path if it exists.</p>
     *
     * @param key the storage key of the object to delete
     * @throws RuntimeException if the deletion from local storage fails
     */
    public void delete(String key) {
        log.debug("delete called with key={}", key);
        if (s3Enabled && s3Client != null) {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.info("Deleted from S3: bucket={}, key={}", bucket, key);
        } else {
            log.info("S3 disabled, using local fallback for delete key={}", key);
            try {
                Files.deleteIfExists(Paths.get(localStoragePath, key));
                log.info("Deleted from local storage: key={}", key);
            } catch (IOException e) {
                log.error("Failed to delete from local storage for key={}", key, e);
                throw new RuntimeException("Failed to delete from local storage", e);
            }
        }
    }

    /**
     * Generates a presigned URL for temporary access to an object.
     *
     * <p>When S3 is enabled, returns an {@code s3://} URI (presigned URL generation
     * with S3Presigner is not yet implemented). When S3 is disabled, returns a
     * {@code local://} URI referencing the local storage key.</p>
     *
     * @param key the storage key of the object to generate a URL for
     * @param expiry the duration for which the presigned URL should be valid
     * @return a URI string pointing to the stored object
     */
    public String generatePresignedUrl(String key, Duration expiry) {
        log.debug("generatePresignedUrl called with key={}, expiry={}", key, expiry);
        if (s3Enabled && s3Client != null) {
            // For presigned URLs, would need S3Presigner — returning S3 URI for now
            return "s3://" + bucket + "/" + key;
        } else {
            log.info("S3 disabled, returning local URI for key={}", key);
            return "local://" + key;
        }
    }
}

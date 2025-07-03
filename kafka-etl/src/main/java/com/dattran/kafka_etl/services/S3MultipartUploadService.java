package com.dattran.kafka_etl.services;

import com.dattran.kafka_etl.s3model.CompletedPartInfo;
import com.dattran.kafka_etl.s3model.MultipartUploadResponse;
import com.dattran.kafka_etl.s3model.PresignedUrlInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3MultipartUploadService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private static final long PART_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int URL_EXPIRATION_MINUTES = 60;

    public S3MultipartUploadService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    // 1. Khởi tạo Multipart Upload
    public MultipartUploadResponse initializeMultipartUpload(String fileName, long fileSize) {
        String key = "uploads/" + fileName;
        int totalParts = (int) Math.ceil((double) fileSize / PART_SIZE);

        // Tạo multipart upload
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
        String uploadId = response.uploadId();

        // Tạo presigned URLs cho tất cả parts
        List<PresignedUrlInfo> presignedUrls = generatePresignedUrls(key, uploadId, totalParts);

        return MultipartUploadResponse.builder()
                .uploadId(uploadId)
                .key(key)
                .totalParts(totalParts)
                .presignedUrls(presignedUrls)
                .build();
    }

    // 2. Tạo Presigned URLs cho từng part
    private List<PresignedUrlInfo> generatePresignedUrls(String key, String uploadId, int totalParts) {
        List<PresignedUrlInfo> urls = new ArrayList<>();

        for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(URL_EXPIRATION_MINUTES))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(presignRequest);

            urls.add(PresignedUrlInfo.builder()
                    .partNumber(partNumber)
                    .url(presignedRequest.url().toString())
                    .build());
        }

        return urls;
    }

    // 3. Complete Multipart Upload - FIXED VERSION
    public void completeMultipartUpload(String key, String uploadId, List<CompletedPartInfo> parts) {
        try {
            // Validate input
            if (parts == null || parts.isEmpty()) {
                throw new IllegalArgumentException("Parts list cannot be null or empty");
            }

            // Sort parts by part number to ensure correct order
            List<CompletedPartInfo> sortedParts = parts.stream()
                    .sorted(Comparator.comparing(CompletedPartInfo::getPartNumber))
                    .collect(Collectors.toList());

            // Validate part sequence
            validatePartSequence(sortedParts);

            // Convert to AWS SDK format with proper ETag handling
            List<CompletedPart> completedParts = sortedParts.stream()
                    .map(part -> {
                        String eTag = normalizeETag(part.getETag());
                        return CompletedPart.builder()
                                .partNumber(part.getPartNumber())
                                .eTag(eTag)
                                .build();
                    })
                    .collect(Collectors.toList());

            CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(completedUpload)
                    .build();

            s3Client.completeMultipartUpload(request);

        } catch (S3Exception e) {
            // If completion fails, abort the upload to avoid charges
            abortMultipartUpload(key, uploadId);
            throw new RuntimeException("Failed to complete multipart upload: " + e.getMessage(), e);
        } catch (Exception e) {
            // For any other error, also abort the upload
            abortMultipartUpload(key, uploadId);
            throw new RuntimeException("Unexpected error during multipart upload completion: " + e.getMessage(), e);
        }
    }

    // Helper method to validate part sequence
    private void validatePartSequence(List<CompletedPartInfo> parts) {
        for (int i = 0; i < parts.size(); i++) {
            int expectedPartNumber = i + 1;
            int actualPartNumber = parts.get(i).getPartNumber();

            if (actualPartNumber != expectedPartNumber) {
                throw new IllegalArgumentException(
                        String.format("Invalid part sequence. Expected part %d but found part %d",
                                expectedPartNumber, actualPartNumber));
            }
        }
    }

    // Helper method to normalize ETag format
    private String normalizeETag(String eTag) {
        if (eTag == null || eTag.trim().isEmpty()) {
            throw new IllegalArgumentException("ETag cannot be null or empty");
        }

        String trimmedETag = eTag.trim();

        // If ETag doesn't have quotes, add them
        if (!trimmedETag.startsWith("\"") || !trimmedETag.endsWith("\"")) {
            // Remove any existing quotes first, then add proper quotes
            trimmedETag = trimmedETag.replace("\"", "");
            trimmedETag = "\"" + trimmedETag + "\"";
        }

        return trimmedETag;
    }

    // 4. Abort Multipart Upload (cleanup)
    public void abortMultipartUpload(String key, String uploadId) {
        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .build();

            s3Client.abortMultipartUpload(request);
        } catch (Exception e) {
            // Log the error but don't throw - abort is cleanup operation
            System.err.println("Failed to abort multipart upload: " + e.getMessage());
        }
    }

    // 5. List parts of a multipart upload (useful for debugging)
    public List<Part> listParts(String key, String uploadId) {
        ListPartsRequest request = ListPartsRequest.builder()
                .bucket(bucketName)
                .key(key)
                .uploadId(uploadId)
                .build();

        ListPartsResponse response = s3Client.listParts(request);
        return response.parts();
    }

    // 6. Helper method to get part info from S3 (for verification)
    public void verifyPartsBeforeCompletion(String key, String uploadId, List<CompletedPartInfo> parts) {
        List<Part> s3Parts = listParts(key, uploadId);

        if (s3Parts.size() != parts.size()) {
            throw new IllegalStateException(
                    String.format("Mismatch in part count. Expected %d parts but S3 has %d parts",
                            parts.size(), s3Parts.size()));
        }

        for (CompletedPartInfo part : parts) {
            boolean found = s3Parts.stream()
                    .anyMatch(s3Part -> s3Part.partNumber() == part.getPartNumber());

            if (!found) {
                throw new IllegalStateException(
                        String.format("Part %d not found in S3", part.getPartNumber()));
            }
        }
    }
}
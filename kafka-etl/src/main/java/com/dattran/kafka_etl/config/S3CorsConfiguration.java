package com.dattran.kafka_etl.config;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.util.Arrays;

@Configuration
public class S3CorsConfiguration {
    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName; // Lấy từ application.properties

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    @EventListener(ApplicationReadyEvent.class)
    public void configureCorsOnStartup() {
        try {
            configureBucketCors();
            System.out.println("✅ S3 CORS configuration applied successfully to bucket: " + bucketName);
        } catch (Exception e) {
            System.err.println("❌ Error configuring S3 CORS: " + e.getMessage());
        }
    }

    private void configureBucketCors() {
        // Tạo CORS rule
        CORSRule corsRule = CORSRule.builder()
                .id("multipart-upload-cors-rule")
                .allowedMethods(Arrays.asList(
                        "GET", "PUT", "POST", "DELETE", "HEAD"
                ))
                .allowedOrigins(Arrays.asList(allowedOrigins))
                .allowedHeaders(Arrays.asList("*"))
                .exposeHeaders(Arrays.asList(
                        "ETag",
                        "x-amz-request-id",
                        "x-amz-version-id",
                        "Content-Length",
                        "Content-Type"
                ))
                .maxAgeSeconds(3600)
                .build();

        // Tạo CORS configuration
        CORSConfiguration corsConfiguration = CORSConfiguration.builder()
                .corsRules(corsRule)
                .build();

        // Apply CORS configuration
        PutBucketCorsRequest putBucketCorsRequest = PutBucketCorsRequest.builder()
                .bucket(bucketName)
                .corsConfiguration(corsConfiguration)
                .build();

        s3Client.putBucketCors(putBucketCorsRequest);
    }
}
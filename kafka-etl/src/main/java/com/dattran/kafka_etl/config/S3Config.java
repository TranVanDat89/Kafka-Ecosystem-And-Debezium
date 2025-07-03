package com.dattran.kafka_etl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
    @Value("${aws.credentials.access-key:}")
    private String accessKey;

    @Value("${aws.credentials.secret-key:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.AP_SOUTHEAST_2)
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.AP_SOUTHEAST_2)
                .credentialsProvider(getCredentialsProvider())
                .build();
    }

    private StaticCredentialsProvider getCredentialsProvider() {
        if (accessKey != null && !accessKey.isEmpty() &&
                secretKey != null && !secretKey.isEmpty()) {
            // Use custom credentials
            AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            return StaticCredentialsProvider.create(credentials);
        } else {
            // Use default credentials chain (IAM roles, environment variables, etc.)
            return StaticCredentialsProvider.create(
                    DefaultCredentialsProvider.create().resolveCredentials()
            );
        }
    }
}

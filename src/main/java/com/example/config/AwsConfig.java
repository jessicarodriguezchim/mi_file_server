package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    // Opción 1: Con valores por defecto (para desarrollo)
    @Value("${aws.access-key:AKIAXXXXXXXXXXXXXXXX}")
    private String accessKey;

    @Value("${aws.secret-key:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX}")
    private String secretKey;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}

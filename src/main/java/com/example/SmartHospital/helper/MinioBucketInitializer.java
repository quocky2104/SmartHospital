package com.example.SmartHospital.helper;

import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MinioBucketInitializer {
    private final MinioClient minioClient;
    @PostConstruct // This method will be called after the bean is initialized
    public void createBucket() {
        try{
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket("avatars").build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("avatars").build());
            }
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket("request-attachments").build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("request-attachments").build());
            }
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket("message-attachments").build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("message-attachments").build());
            }
        } catch (Exception e) {
            // Log the error and rethrow as a runtime exception to prevent application from starting
            System.err.println("Error initializing Minio buckets: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Minio buckets", e);
        }
        
    }
}

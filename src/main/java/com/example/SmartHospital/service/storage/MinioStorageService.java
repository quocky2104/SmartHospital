package com.example.SmartHospital.service.storage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioStorageService {
    // UUID for generating unique file names, and sanitization to prevent issues with special characters in filenames

    private final MinioClient minioClient;

    @Value("${minio.bucket:avatars}")
    private String avatarBucket;

    @Value("${minio.medical-record-bucket:medicalrecord-attachments}")
    private String medicalRecordBucket;

    @Value("${minio.additional-file-bucket:request-attachments}")
    private String additionalFileBucket;

    @Value("${minio.chat-file-bucket:chat-files}")
    private String chatFileBucket;

    @Value("${minio.presigned-expiry-seconds:3600}")
    private Integer presignedExpirySeconds;

    // ex: url name: "chat-files/userId/550e8400-e29b-41d4-a716-446655440000-originalfilename.ext"
    public String uploadChatFile(MultipartFile chatFile, String userId) {
        if (chatFile == null || chatFile.isEmpty()) {
            return null;
        }

        String contentType = chatFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        String objectName = userId + "/" + UUID.randomUUID() + "-" + sanitizeFileName(chatFile.getOriginalFilename());
        uploadFile(chatFileBucket, objectName, chatFile, contentType);
        return chatFileBucket + "/" + objectName;
    }

    // ex: url name: "avatars/userId/avatar.png"
    public String uploadAvatar(MultipartFile avatarFile, String userId) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            return null;
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image file");
        }

        String extension = resolveFileExtension(avatarFile.getOriginalFilename(), "png");
        String objectName = userId + "/avatar." + extension;
        uploadFile(avatarBucket, objectName, avatarFile, contentType);
        cleanupOldAvatarVariants(userId, objectName);
        return avatarBucket + "/" + objectName;
    }

    // Generates a presigned GET URL from a stored path like "bucket/object..."
    public String toPresignedGetUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }

        String normalized = storedPath.startsWith("/") ? storedPath.substring(1) : storedPath;
        String[] parts = normalized.split("/", 2);
        if (parts.length != 2) {
            return storedPath;
        }

        int expiry = presignedExpirySeconds == null || presignedExpirySeconds <= 0
            ? 3600
            : presignedExpirySeconds;

        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(parts[0])
                    .object(parts[1])
                    .expiry(expiry)
                    .build()
            );
        } catch (Exception e) {
            throw new MinioUploadException("Failed to generate presigned URL", e);
        }
    }

    // ex: url name: "medicalrecord-attachments/userId/550e8400-e29b-41d4-a716-446655440000.pdf"
    public List<String> uploadMedicalRecordPdfs(List<MultipartFile> medicalRecordFiles, String userId) {
        List<String> uploadedPaths = new ArrayList<>();
        if (medicalRecordFiles == null || medicalRecordFiles.isEmpty()) {
            return uploadedPaths;
        }

        for (MultipartFile file : medicalRecordFiles) {
            if (file != null && !file.isEmpty()) {
                String contentType = file.getContentType();
                String originalFileName = file.getOriginalFilename();
                String originalName = originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
                boolean isPdf = "application/pdf".equalsIgnoreCase(contentType) || originalName.endsWith(".pdf");
                if (!isPdf) {
                    throw new IllegalArgumentException("Medical record files must be PDF");
                }

                String objectName = userId + "/" + UUID.randomUUID() + ".pdf";
                uploadFile(medicalRecordBucket, objectName, file, "application/pdf");
                uploadedPaths.add(medicalRecordBucket + "/" + objectName);
            }
        }

        return uploadedPaths;
    }

    // ex: url name: "request-attachments/userId/550e8400-e29b-41d4-a716-446655440000-originalfilename.ext"
    public List<String> uploadAdditionalFiles(List<MultipartFile> additionalFiles, String userId) {
        List<String> uploadedPaths = new ArrayList<>();
        if (additionalFiles == null || additionalFiles.isEmpty()) {
            return uploadedPaths;
        }

        for (MultipartFile file : additionalFiles) {
            if (file != null && !file.isEmpty()) {
                String contentType = file.getContentType();
                if (contentType == null || contentType.isBlank()) {
                    contentType = "application/octet-stream";
                }

                // Ex: userId/550e8400-e29b-41d4-a716-446655440000-originalfilename.ext
                String objectName = userId + "/" + UUID.randomUUID() + "-" + sanitizeFileName(file.getOriginalFilename());
                uploadFile(additionalFileBucket, objectName, file, contentType);
                uploadedPaths.add(additionalFileBucket + "/" + objectName);
            }
        }

        return uploadedPaths;
    }

    public void deleteFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return;
        }

        for (String filePath : filePaths) {
            if (filePath != null && !filePath.isBlank()) {
                String[] parts = filePath.split("/", 2);
                if (parts.length == 2) {
                    try {
                        minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                .bucket(parts[0])
                                .object(parts[1])
                                .build()
                        );
                    } catch (Exception e) {
                        throw new MinioUploadException("Failed to delete file from MinIO", e);
                    }
                }
            }
        }
    }

    private void uploadFile(String bucket, String objectName, MultipartFile file, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
        } catch (Exception e) {
            throw new MinioUploadException("Failed to upload file to MinIO", e);
        }
    }

    private void cleanupOldAvatarVariants(String userId, String keepObjectName) {
        String avatarPrefix = userId + "/avatar.";
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(avatarBucket)
                    .prefix(avatarPrefix)
                    .recursive(true)
                    .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();
                if (!keepObjectName.equals(objectName)) {
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(avatarBucket)
                            .object(objectName)
                            .build()
                    );
                }
            }
        } catch (Exception e) {
            throw new MinioUploadException("Failed to clean up old avatar files", e);
        }
    }

    // Resolves file extension from original filename, defaults to provided default if not found
    private String resolveFileExtension(String fileName, String defaultExt) {
        if (fileName == null || !fileName.contains(".")) {
            return defaultExt;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    // Sanitizes filename by replacing non-alphanumeric characters with underscores
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // Custom runtime exception for MinIO upload errors
    public static class MinioUploadException extends RuntimeException {
        public MinioUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
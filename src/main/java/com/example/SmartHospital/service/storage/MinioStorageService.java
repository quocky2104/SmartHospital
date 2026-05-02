package com.example.SmartHospital.service.storage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MinioStorageService {
    private final MinioClient minioClient;

    @Value("${minio.bucket:avatars}")
    private String avatarBucket;

    @Value("${minio.medical-record-bucket:medicalrecord-attachments}")
    private String medicalRecordBucket;

    @Value("${minio.additional-file-bucket:request-attachments}")
    private String additionalFileBucket;

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
        return avatarBucket + "/" + objectName;
    }

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

    private String resolveFileExtension(String fileName, String defaultExt) {
        if (fileName == null || !fileName.contains(".")) {
            return defaultExt;
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static class MinioUploadException extends RuntimeException {
        public MinioUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
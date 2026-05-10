package com.example.SmartHospital.service.socialPost;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.SocialDtos.Request.SocialPostRequest;
import com.example.SmartHospital.model.SocialPost;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.SocialPostRepository;
import com.example.SmartHospital.repository.UserRepository;
import com.example.SmartHospital.service.storage.MinioStorageService;
import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SocialPostService {
    private final SocialPostRepository socialPostRepository;
    private final UserRepository userRepository;
    private final MinioStorageService minioStorageService;
    private final EntityManager entityManager;

    private SocialPost populatePresignedUrls(SocialPost post) {
        if (post == null) return null;
        if (post.getLikedByUserIds() != null) {
            post.getLikedByUserIds().size();
        }
        entityManager.detach(post);
        if (post.getCoverImage() != null && !post.getCoverImage().startsWith("http")) {
            post.setCoverImage(minioStorageService.toPresignedGetUrl(post.getCoverImage()));
        }
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            List<String> presigned = new ArrayList<>();
            for (String url : post.getImageUrls()) {
                if (url != null && !url.startsWith("http")) {
                    presigned.add(minioStorageService.toPresignedGetUrl(url));
                } else {
                    presigned.add(url);
                }
            }
            post.setImageUrls(presigned);
        }
        if (post.getUserId() != null) {
            entityManager.detach(post.getUserId());
            if (post.getUserId().getAvatarPath() != null && !post.getUserId().getAvatarPath().startsWith("http")) {
                post.getUserId().setAvatarPath(minioStorageService.toPresignedGetUrl(post.getUserId().getAvatarPath()));
            }
        }
        return post;
    }

    public SocialPost createPost(String userId, SocialPostRequest request, List<MultipartFile> images, MultipartFile coverImageFile) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        SocialPost post = new SocialPost();
        post.setUserId(user);
        // If images provided, upload and use returned paths; otherwise use provided imageUrls
        if (images != null && !images.isEmpty()) {
            List<String> uploaded = minioStorageService.uploadAdditionalFiles(images, userId);
            post.setImageUrls(uploaded);
        } else if (request != null && request.getImageUrls() != null) {
            post.setImageUrls(request.getImageUrls());
        }
        // Preserve format: don't manipulate content - store as-is
        if (request != null) {
            post.setTitle(request.getTitle());
            post.setShortDescription(request.getShortDescription());
            
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                List<String> coverUploaded = minioStorageService.uploadAdditionalFiles(List.of(coverImageFile), userId);
                if (coverUploaded != null && !coverUploaded.isEmpty()) {
                    post.setCoverImage(coverUploaded.get(0));
                }
            } else if (request.getCoverImage() != null) {
                post.setCoverImage(request.getCoverImage());
            }
            
            String content = request.getContent();
            post.setContent(content != null ? content : "");
        } else {
            post.setContent("");
        }
        post = socialPostRepository.save(post);
        return populatePresignedUrls(post);
    }

    public SocialPost getPostById(String postId) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return populatePresignedUrls(post);
    }

    public Page<SocialPost> getAllPosts(int pageNumber, int pageSize, String search, String sortBy) {
        Sort.Direction direction = "newest".equalsIgnoreCase(sortBy) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));

        if (search != null && !search.isEmpty()) {
            return socialPostRepository.searchByContent(search, pageRequest).map(this::populatePresignedUrls);
        }
        return socialPostRepository.findAll(pageRequest).map(this::populatePresignedUrls);
    }

    public List<SocialPost> getAllPosts() {
        return socialPostRepository.findAll().stream().map(this::populatePresignedUrls).toList();
    }

    public SocialPost updatePost(String postId, SocialPostRequest request, List<MultipartFile> images, MultipartFile coverImageFile, String userId) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (images != null && !images.isEmpty()) {
            List<String> uploaded = minioStorageService.uploadAdditionalFiles(images, userId);
            post.setImageUrls(uploaded);
        } else if (request != null && request.getImageUrls() != null) {
            post.setImageUrls(request.getImageUrls());
        }
        // Preserve format: don't manipulate content - store as-is
        if (request != null) {
            post.setTitle(request.getTitle());
            post.setShortDescription(request.getShortDescription());
            
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                List<String> coverUploaded = minioStorageService.uploadAdditionalFiles(List.of(coverImageFile), userId);
                if (coverUploaded != null && !coverUploaded.isEmpty()) {
                    post.setCoverImage(coverUploaded.get(0));
                }
            } else if (request.getCoverImage() != null) {
                post.setCoverImage(request.getCoverImage());
            }

            post.setContent(request.getContent() != null ? request.getContent() : "");
        }
        post.setUpdatedAt(java.time.LocalDateTime.now());
        post = socialPostRepository.save(post);
        return populatePresignedUrls(post);
    }

    public void deletePost(String postId) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        socialPostRepository.delete(post);
    }

    public SocialPost toggleLike(String postId, String userId) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (post.getLikedByUserIds().contains(userId)) {
            post.getLikedByUserIds().remove(userId);
        } else {
            post.getLikedByUserIds().add(userId);
        }
        return socialPostRepository.save(post);
    }
}

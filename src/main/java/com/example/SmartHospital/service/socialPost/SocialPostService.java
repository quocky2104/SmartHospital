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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SocialPostService {
    private final SocialPostRepository socialPostRepository;
    private final UserRepository userRepository;
    private final MinioStorageService minioStorageService;

    public SocialPost createPost(String userId, SocialPostRequest request, List<MultipartFile> images) {
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
        String content = request != null ? request.getContent() : null;
        post.setContent(content != null ? content : "");
        return socialPostRepository.save(post);
    }

    public SocialPost getPostById(String postId) {
        return socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public Page<SocialPost> getAllPosts(int pageNumber, int pageSize, String search, String sortBy) {
        Sort.Direction direction = "newest".equalsIgnoreCase(sortBy) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));

        if (search != null && !search.isEmpty()) {
            return socialPostRepository.searchByContent(search, pageRequest);
        }
        return socialPostRepository.findAll(pageRequest);
    }

    public List<SocialPost> getAllPosts() {
        return socialPostRepository.findAll();
    }

    public SocialPost updatePost(String postId, SocialPostRequest request) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setImageUrls(request.getImageUrls());
        // Preserve format: don't manipulate content - store as-is
        post.setContent(request.getContent() != null ? request.getContent() : "");
        post.setUpdatedAt(java.time.LocalDateTime.now());
        return socialPostRepository.save(post);
    }

    public void deletePost(String postId) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        socialPostRepository.delete(post);
    }
}

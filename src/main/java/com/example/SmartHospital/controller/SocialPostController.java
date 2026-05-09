package com.example.SmartHospital.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.SocialDtos.Request.SocialPostRequest;
import com.example.SmartHospital.model.SocialComment;
import com.example.SmartHospital.model.SocialPost;
import com.example.SmartHospital.service.socialPost.SocialCommentService;
import com.example.SmartHospital.service.socialPost.SocialPostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/social-posts")
@RequiredArgsConstructor
public class SocialPostController {
    private final SocialPostService socialPostService;
    private final SocialCommentService socialCommentService;

    @Operation(summary = "Create social post", description = "Only Admins can create social posts that will be visible to public (even non-authenticated users).")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SocialPost>> createPost(
        @AuthenticationPrincipal String userId,
        @RequestPart(required = false) SocialPostRequest request,
        @RequestPart(required = false) List<MultipartFile> images
    ) {
        SocialPost createdPost = socialPostService.createPost(userId, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "Social post created", createdPost));
    }

    @Operation(summary = "List social posts", description = "Anyone can view the list of social posts, including non-authenticated users.")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PaginatedResponse<SocialPost>>> listPosts(
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int pageNumber,
        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "10") int pageSize,
        @Parameter(description = "Search by content")
        @RequestParam(required = false) String search,
        @Parameter(description = "Sort by (createdAt)")
        @RequestParam(required = false) String sortBy
        
    ) {
        org.springframework.data.domain.Page<SocialPost> page = socialPostService.getAllPosts(pageNumber, pageSize, search, sortBy);
        PaginatedResponse<SocialPost> paginatedResponse = new PaginatedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
        return ResponseEntity.ok(new ApiResponse<>(200, "Social posts retrieved", paginatedResponse));
    }

    @Operation(summary = "Update social post", description = "Only Admins can update social posts.")
    @PutMapping("/{postId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SocialPost>> updatePost(@AuthenticationPrincipal String userId, @PathVariable String postId, @RequestBody SocialPostRequest request) {
        SocialPost updatedPost = socialPostService.updatePost(postId, request);
        return ResponseEntity.ok(new ApiResponse<>(200, "Social post updated", updatedPost));
    }

    @Operation(summary = "Delete social post", description = "Only Admins can delete social posts.")
    @DeleteMapping("/{postId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(@AuthenticationPrincipal String userId, @PathVariable String postId) {
        socialPostService.deletePost(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Social post deleted", null));
    }


    @Operation(summary = "Get social post by ID", description = "Anyone can view a specific social post, including non-authenticated users.")
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<SocialPost>> getPostById(@PathVariable String postId) {
        SocialPost post = socialPostService.getPostById(postId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Social post retrieved", post));
    }

    @Operation(summary = "Add comment to social post", description = "Only authenticated users can comment on social posts.")
    @PostMapping("/{postId}/comments/add")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')") // Allow all authenticated users to comment
    public ResponseEntity<ApiResponse<SocialComment>> addComment(
        @AuthenticationPrincipal String userId,
        @PathVariable String postId, 
        @RequestParam String content
    ) {
        SocialComment createdComment = socialCommentService.createComment(userId, postId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "Comment added", createdComment));
    }

    @Operation(summary = "Get comments for social post", description = "Anyone can view comments of a social post, including non-authenticated users.")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ApiResponse<PaginatedResponse<SocialComment>>> getComments(
        @PathVariable String postId,
        @Parameter(description = "Page number (0-indexed)")
        @RequestParam(defaultValue = "0") int pageNumber,
        @Parameter(description = "Page size (max 100)")
        @RequestParam(defaultValue = "10") int pageSize,
        @Parameter(description = "Sort by (createdAt)")
        @RequestParam(required = false) String sortNewestLatest
    ) {
        org.springframework.data.domain.Page<SocialComment> page = socialCommentService.getCommentsForPost(postId, pageNumber, pageSize, sortNewestLatest);
        PaginatedResponse<SocialComment> paginatedResponse = new PaginatedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
        return ResponseEntity.ok(new ApiResponse<>(200, "Comments retrieved", paginatedResponse));
    }

    @Operation(summary = "Delete comment", description = "Users can delete their own comments, and Admins can delete any comment.")
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@AuthenticationPrincipal String userId, @PathVariable String commentId) {
        try {
            socialCommentService.deleteComment(userId, commentId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Comment deleted", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(403, e.getMessage(), null));
        }
    }

    @Operation(summary = "Update comment", description = "Users can update their own comments, and Admins can update any comment.")
    @PutMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<SocialComment>> updateComment(
        @AuthenticationPrincipal String userId,
        @PathVariable String commentId,
        @RequestParam String content
    ) {
        try {
            SocialComment updatedComment = socialCommentService.updateComment(userId, commentId, content);
            return ResponseEntity.ok(new ApiResponse<>(200, "Comment updated", updatedComment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(403, e.getMessage(), null));
        }
    }

}

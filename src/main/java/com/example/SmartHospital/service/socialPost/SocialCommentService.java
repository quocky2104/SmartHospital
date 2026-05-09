package com.example.SmartHospital.service.socialPost;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.example.SmartHospital.repository.SocialPostRepository;
import com.example.SmartHospital.repository.SocialCommentRepository;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.model.SocialComment;
import com.example.SmartHospital.model.SocialPost;
import com.example.SmartHospital.repository.UserRepository;
import com.example.SmartHospital.enums.RoleType;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialCommentService {
    private final SocialCommentRepository socialCommentRepository;
    private final SocialPostRepository socialPostRepository;
    private final UserRepository userRepository;

    public SocialComment createComment(String userId, String postId, String content) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        SocialComment comment = new SocialComment();
        comment.setPostId(post);
        comment.setUserId(user);
        // Preserve format: don't manipulate content
        comment.setContent(content != null ? content : "");
        return socialCommentRepository.save(comment);
    }

    public List<SocialComment> getCommentsForPost(String postId, int pageNumber, int pageSize, String sortBy) {
        SocialPost post = socialPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        Sort.Direction direction = "newest".equalsIgnoreCase(sortBy) ? Sort.Direction.DESC : Sort.Direction.ASC;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(direction, "createdAt"));
        
        return socialCommentRepository.findByPostId(post, pageRequest).getContent();
    }

    public void deleteComment(String userId, String commentId) {
        SocialComment comment = socialCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        // Only allow deletion if the user is the comment author or has admin role
        if (!comment.getUserId().getId().equals(userId) &&
            user.getRole() != RoleType.ADMIN
         ) {
            throw new IllegalArgumentException("User not authorized to delete this comment");
        }
        socialCommentRepository.delete(comment);
    }

    public SocialComment updateComment(String userId, String commentId, String newContent) {
        SocialComment comment = socialCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getUserId().getId().equals(userId)) {
            throw new IllegalArgumentException("User not authorized to update this comment");
        }
        // Preserve format: don't manipulate content
        comment.setContent(newContent != null ? newContent : "");
        return socialCommentRepository.save(comment);
    }
}
package com.example.SmartHospital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.example.SmartHospital.model.SocialComment;
import com.example.SmartHospital.model.SocialPost;

@Repository
public interface SocialCommentRepository extends JpaRepository<SocialComment, String> {
    Page<SocialComment> findByPostId(SocialPost postId, Pageable pageable);
}

package com.example.SmartHospital.model;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.JoinColumn;

import java.time.LocalDateTime;

import com.example.SmartHospital.helper.CustomIdGenerator;

import jakarta.persistence.Column;


@Data
@Entity
@Table(name = "social_comment")
public class SocialComment {
    @Id
    private String id;

    // Use TEXT column type for longer comments and save the format
    @Column(nullable=false, columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne
    @JoinColumn(name="post_id")
    private SocialPost postId;
    
    @ManyToOne
    @JoinColumn(name="user_id")
    private User userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generateCommentId();
        }
        this.createdAt = LocalDateTime.now();
    }
}

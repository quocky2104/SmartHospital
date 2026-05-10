package com.example.SmartHospital.model;

import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.JoinColumn;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import com.example.SmartHospital.helper.CustomIdGenerator;
@Entity
@Table(name = "social_post")
@Data
public class SocialPost {
    @Id
    private String id;
    @Column(nullable=true)
    private String title;

    @Column(nullable=true, columnDefinition = "TEXT")
    private String shortDescription;

    @Column(nullable=true)
    private String coverImage;

    @Column(nullable=false, columnDefinition = "TEXT")
    private String content;
    
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable=true, columnDefinition = "JSON")
    private List<String> imageUrls;

    @ElementCollection
    @CollectionTable(name = "social_post_likes", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "user_id")
    private Set<String> likedByUserIds = new HashSet<>();

    @ManyToOne
    @JoinColumn(name="user_id")
    private User userId;

    @Column(nullable=false)
    private LocalDateTime createdAt;
    @Column(nullable=true)
    private LocalDateTime updatedAt;

    @PrePersist
    public void createIdIfNotPresent() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = CustomIdGenerator.generatePostId();
        }
        this.createdAt = LocalDateTime.now();
    }
}

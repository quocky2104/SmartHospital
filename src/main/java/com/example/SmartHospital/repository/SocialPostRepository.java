package com.example.SmartHospital.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.SmartHospital.model.SocialPost;

@Repository
public interface SocialPostRepository extends JpaRepository<SocialPost, String> {
    @Query("SELECT p FROM SocialPost p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<SocialPost> searchByContent(@Param("search") String search, Pageable pageable);
}

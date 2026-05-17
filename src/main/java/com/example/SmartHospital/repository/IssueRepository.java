package com.example.SmartHospital.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.enums.IssueStatus;
import com.example.SmartHospital.model.Admin;
import com.example.SmartHospital.model.Issue;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    List<Issue> findByReporterId(String reporterId);

    List<Issue> findByReporterIdAndIsDeletedFalse(String reporterId);

    List<Issue> findByStatusAndIsDeletedFalse(IssueStatus status);

    List<Issue> findByAssignedAdminAndIsDeletedFalse(Admin admin);

    @Query("SELECT i FROM Issue i WHERE i.isDeleted = false ORDER BY i.createdAt DESC")
    List<Issue> findAllActiveIssues();

    @Query("SELECT i FROM Issue i WHERE i.status = :status AND i.isDeleted = false ORDER BY i.createdAt DESC")
    List<Issue> findByStatusOrderByCreatedAtDesc(@Param("status") IssueStatus status);

    Optional<Issue> findByIdAndIsDeletedFalse(String id);
    
    // Pageable methods for admin
    Page<Issue> findByIsDeletedFalse(Pageable pageable);
    
    Page<Issue> findByTitleContainingIgnoreCaseAndIsDeletedFalse(String title, Pageable pageable);
    
    Page<Issue> findByStatusAndIsDeletedFalse(IssueStatus status, Pageable pageable);

    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(IssueStatus status);

    long countByCreatedAtBetweenAndIsDeletedFalse(java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<Issue> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();
}

package com.example.SmartHospital.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
    List<Notification> findByRecipientUserIdAndIsReadFalse(String recipientUserId);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.recipientUserId = :recipientUserId
          AND (n.isRead = false OR n.isRead IS NULL)
    """)
    int markAllAsRead(@Param("recipientUserId") String recipientUserId);

    @Query("""
        SELECT COUNT(n)
        FROM Notification n
        WHERE n.recipientUserId = :recipientUserId
          AND (n.isRead = false OR n.isRead IS NULL)
    """)
    long countUnread(@Param("recipientUserId") String recipientUserId);
}
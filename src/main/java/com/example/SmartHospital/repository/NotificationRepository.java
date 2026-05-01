package com.example.SmartHospital.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);
    List<Notification> findByRecipientUserIdAndIsReadFalse(String recipientUserId);
    long countByRecipientUserIdAndIsReadFalse(String recipientUserId);
}
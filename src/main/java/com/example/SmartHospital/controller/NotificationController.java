package com.example.SmartHospital.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.NotificationDtos.NotificationResponse;
import com.example.SmartHospital.service.notification.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
	private final NotificationService notificationService;

    @Operation(summary = "Get current user's notifications")
	@GetMapping("/my")
	public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(new ApiResponse<>(200, "Notifications fetched", notificationService.getMyNotifications(userId)));
	}

	@Operation(
		summary = "Get unread notification count",
		description = "Get the total count of unread notifications for the authenticated user"
	)
	@GetMapping("/my/unread-count")
	public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(new ApiResponse<>(200, "Unread count fetched", notificationService.getUnreadCount(userId)));
	}

	@Operation(
		summary = "Mark notification as read",
		description = "Mark a specific notification as read for the authenticated user"
	)
	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(
		@AuthenticationPrincipal String userId,
		@PathVariable String notificationId
	) {
		boolean ok = notificationService.markAsRead(userId, notificationId);
		if (!ok) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Notification not found", null));
		}
		return ResponseEntity.ok(new ApiResponse<>(200, "Notification marked as read", null));
	}

    @Operation(
        summary = "Mark all notifications as read",
        description = "Mark all notifications as read for the authenticated user in a single operation"
    )
    @PatchMapping("/my/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal String userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(new ApiResponse<>(200, "All notifications marked as read", null));
    }

    @Operation(
        summary = "Mark notification as unread",
        description = "Mark a specific notification as unread for the authenticated user"
    )
    @PatchMapping("/{notificationId}/unread")
    public ResponseEntity<ApiResponse<Void>> markAsUnread(
        @AuthenticationPrincipal String userId,
        @PathVariable String notificationId
    ) {
        notificationService.markAsUnread(userId, notificationId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Notification marked as unread", null));
    }
}

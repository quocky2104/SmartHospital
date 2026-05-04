package com.example.SmartHospital.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.UserDtos.AdminProfileDTO;
import com.example.SmartHospital.model.User;
import com.example.SmartHospital.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/admin/user-profile")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminProfileController {
    private final UserRepository userRepository;

    @Operation(
        summary = "View admin profile",
        description = "Retrieve the complete profile information of the authenticated admin"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/view")
    public ResponseEntity<ApiResponse<AdminProfileDTO>> viewAdminProfile(@AuthenticationPrincipal String userId) {
        return userRepository.findById(userId)
            .map(this::toResponse)
            .orElseGet(() -> ResponseEntity.badRequest().body(new ApiResponse<>(400, "Failed to retrieve admin profile", null)));
    }

    private ResponseEntity<ApiResponse<AdminProfileDTO>> toResponse(User user) {
        return ResponseEntity.ok(
            new ApiResponse<>(200, "Successfully retrieved admin profile", AdminProfileDTO.fromUser(user))
        );
    }
}

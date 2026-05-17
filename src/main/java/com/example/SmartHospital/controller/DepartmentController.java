package com.example.SmartHospital.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.DepartmentDto;
import com.example.SmartHospital.model.Department;
import com.example.SmartHospital.service.department.DepartmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @PreAuthorize("isAuthenticated()") // Allow any authenticated user (patients included) to view departments
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> listDepartments() {
        List<DepartmentDto> list = departmentService.listDepartments();
        return ResponseEntity.ok(new ApiResponse<>(200, "Departments retrieved", list));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listDepartmentsForAdmin(
        @RequestParam(defaultValue = "0") int pageNumber,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String search
    ) {
        Map<String, Object> response = departmentService.listDepartmentsForAdmin(pageNumber, pageSize, search);
        return ResponseEntity.ok(new ApiResponse<>(200, "Departments retrieved", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Department name is required", null));
        }
        try {
            DepartmentDto dto = departmentService.createDepartment(name);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "Department created successfully", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(409, e.getMessage(), null));
        }
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(
            @PathVariable String departmentId,
            @RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Department name is required", null));
        }
        try {
            DepartmentDto dto = departmentService.updateDepartment(departmentId, name);
            return ResponseEntity.ok(new ApiResponse<>(200, "Department updated successfully", dto));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(409, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> softDeleteDepartment(@PathVariable String departmentId) {
        try {
            departmentService.softDeleteDepartment(departmentId);
            return ResponseEntity.ok(new ApiResponse<>(200, "Department deleted successfully", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }
}

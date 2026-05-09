package com.example.SmartHospital.controller;

import java.util.LinkedHashMap;
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
import org.springframework.web.bind.annotation.RestController;

import com.example.SmartHospital.dtos.AuthDtos.Response.ApiResponse;
import com.example.SmartHospital.dtos.DepartmentDto;
import com.example.SmartHospital.model.Department;
import com.example.SmartHospital.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentRepository departmentRepository;

    @PreAuthorize("isAuthenticated()") // Allow any authenticated user (patients included) to view departments
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> listDepartments() {
        List<DepartmentDto> list = departmentRepository.findAllByIsDeletedFalse()
            .stream()
            .map(d -> new DepartmentDto(d.getId(), d.getName()))
            .toList();
        return ResponseEntity.ok(new ApiResponse<>(200, "Departments retrieved", list));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listDepartmentsForAdmin() {
        List<Map<String, Object>> list = departmentRepository.findAll()
            .stream()
            .map(d -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", d.getId());
                item.put("name", d.getName());
                item.put("isDeleted", d.getIsDeleted());
                return item;
            })
            .toList();
        return ResponseEntity.ok(new ApiResponse<>(200, "Departments retrieved", list));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Department name is required", null));
        }

        if (departmentRepository.findByName(name).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(409, "Department name already exists", null));
        }

        Department department = new Department();
        department.setName(name);
        Department saved = departmentRepository.save(department);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(201, "Department created successfully", new DepartmentDto(saved.getId(), saved.getName())));
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(
            @PathVariable String departmentId,
            @RequestBody Map<String, String> request) {
        Department department = departmentRepository.findByIdAndIsDeletedFalse(departmentId).orElse(null);
        if (department == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, "Department not found", null));
        }

        String name = request.getOrDefault("name", "").trim();
        if (name.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Department name is required", null));
        }

        Department existing = departmentRepository.findByName(name).orElse(null);
        if (existing != null && !existing.getId().equals(departmentId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(409, "Department name already exists", null));
        }

        department.setName(name);
        Department saved = departmentRepository.save(department);
        return ResponseEntity.ok(new ApiResponse<>(200, "Department updated successfully", new DepartmentDto(saved.getId(), saved.getName())));
    }

    @DeleteMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> softDeleteDepartment(@PathVariable String departmentId) {
        Department department = departmentRepository.findByIdAndIsDeletedFalse(departmentId).orElse(null);
        if (department == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, "Department not found or already deleted", null));
        }

        department.setIsDeleted(true);
        departmentRepository.save(department);
        return ResponseEntity.ok(new ApiResponse<>(200, "Department deleted successfully", null));
    }
}

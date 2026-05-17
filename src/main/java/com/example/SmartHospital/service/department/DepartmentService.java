package com.example.SmartHospital.service.department;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.SmartHospital.dtos.DepartmentDto;
import com.example.SmartHospital.model.Department;
import com.example.SmartHospital.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public List<DepartmentDto> listDepartments() {
        return departmentRepository.findAllByIsDeletedFalse()
            .stream()
            .map(d -> new DepartmentDto(d.getId(), d.getName()))
            .toList();
    }

    public Map<String, Object> listDepartmentsForAdmin(int pageNumber, int pageSize, String search) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "name"));
        Page<Department> page;

        if (search != null && !search.isBlank()) {
            page = departmentRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(search, pageable);
        } else {
            page = departmentRepository.findAll(pageable);
        }

        List<Map<String, Object>> list = page.getContent()
            .stream()
            .map(d -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", d.getId());
                item.put("name", d.getName());
                item.put("isDeleted", d.getIsDeleted());
                return item;
            })
            .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", list);
        response.put("pageNumber", pageNumber);
        response.put("pageSize", pageSize);
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("isLast", page.isLast());
        return response;
    }

    public DepartmentDto createDepartment(String name) {
        if (departmentRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Department name already exists");
        }
        Department department = new Department();
        department.setName(name);
        Department saved = departmentRepository.save(department);
        return new DepartmentDto(saved.getId(), saved.getName());
    }

    public DepartmentDto updateDepartment(String departmentId, String name) {
        Department department = departmentRepository.findByIdAndIsDeletedFalse(departmentId).orElse(null);
        if (department == null) throw new IllegalArgumentException("Department not found");

        Department existing = departmentRepository.findByName(name).orElse(null);
        if (existing != null && !existing.getId().equals(departmentId)) {
            throw new IllegalArgumentException("Department name already exists");
        }

        department.setName(name);
        Department saved = departmentRepository.save(department);
        return new DepartmentDto(saved.getId(), saved.getName());
    }

    public void softDeleteDepartment(String departmentId) {
        Department department = departmentRepository.findByIdAndIsDeletedFalse(departmentId).orElse(null);
        if (department == null) throw new IllegalArgumentException("Department not found or already deleted");
        department.setIsDeleted(true);
        departmentRepository.save(department);
    }
}

package com.example.SmartHospital.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.SmartHospital.model.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    Optional<Department> findByName(String name);
    List<Department> findAllByIsDeletedFalse();
    Optional<Department> findByIdAndIsDeletedFalse(String id);
}

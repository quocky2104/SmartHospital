package com.example.SmartHospital.service.doctor;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.SmartHospital.dtos.PaginatedResponse;
import com.example.SmartHospital.dtos.UserDtos.DoctorCreateRequest;
import com.example.SmartHospital.dtos.UserDtos.DoctorDTO;
import com.example.SmartHospital.dtos.UserDtos.EditProfile.DoctorEditProfileRequest;
import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Department;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.repository.DepartmentRepository;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.UserRepository;
import com.example.SmartHospital.service.storage.MinioStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorManagementService {
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioStorageService minioStorageService;

    public PaginatedResponse<DoctorDTO> getDoctors(int pageNumber, int pageSize, String search) {
        if (pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        if (pageSize > 10) {
            pageSize = 10;
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Doctor> doctorPage;
        if (search != null && !search.trim().isEmpty()) {
            doctorPage = doctorRepository.searchDoctors(search.trim(), pageable);
        } else {
            doctorPage = doctorRepository.findAll(pageable);
        }

        List<DoctorDTO> content = doctorPage.getContent().stream()
            .filter(doctor -> doctor.getStatus() != UserStatus.DELETED)
            .map(this::convertToDoctorDTO)
            .toList();

        return new PaginatedResponse<>(
            content,
            doctorPage.getNumber(),
            doctorPage.getSize(),
            doctorPage.getTotalElements(),
            doctorPage.getTotalPages(),
            doctorPage.isLast()
        );
    }

    public DoctorDTO getDoctorById(String id) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null || doctor.getStatus() == UserStatus.DELETED) {
            return null;
        }
        return convertToDoctorDTO(doctor);
    }

    public DoctorDTO createDoctor(DoctorCreateRequest request) {
        // Validate unique constraints
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
        if (userRepository.existsByIdentityNumber(request.getIdentityNumber())) {
            throw new IllegalArgumentException("Identity number already exists");
        }

        // Get department
        Department department = departmentRepository.findById(request.getDepartmentId())
            .orElseThrow(() -> new IllegalArgumentException("Department not found"));

        // Create doctor
        Doctor doctor = new Doctor();
        doctor.setFullName(request.getFullName());
        doctor.setEmail(request.getEmail());
        doctor.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setIdentityNumber(request.getIdentityNumber());
        doctor.setGender(request.getGender());
        doctor.setDateOfBirth(request.getDateOfBirth());
        doctor.setAddress(request.getAddress());
        doctor.setWorkingHours(request.getWorkingHours());
        doctor.setAvailabilityStatus(request.getAvailabilityStatus());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setRole(RoleType.DOCTOR);
        doctor.setStatus(UserStatus.ACTIVE);
        doctor.setDepartment(department);

        doctorRepository.save(doctor);
        return convertToDoctorDTO(doctor);
    }

    public DoctorDTO editDoctorProfile(DoctorEditProfileRequest request, String userId, MultipartFile avatarFile) {
        Doctor doctor = doctorRepository.findById(userId).orElse(null);
        if (doctor == null || doctor.getStatus() == UserStatus.DELETED) {
            return null;
        }
        doctor.setFullName(request.getFullName());
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setDateOfBirth(request.getDateOfBirth());
        doctor.setAddress(request.getAddress());
        String avatarPath = minioStorageService.uploadAvatar(avatarFile, doctor.getId());
        if (avatarPath != null) {
            doctor.setAvatarPath(avatarPath);
        }
        doctor.setWorkingHours(request.getWorkingHours());
        doctor.setAvailabilityStatus(request.getAvailabilityStatus());
        doctor.setSpecialization(request.getSpecialization());
        doctorRepository.save(doctor);
        return convertToDoctorDTO(doctor);
    }

    public boolean softDeleteDoctor(String doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null || doctor.getStatus() == UserStatus.DELETED) {
            return false;
        }
        doctor.setStatus(UserStatus.DELETED);
        doctor.setIsDeleted(true);
        doctorRepository.save(doctor);
        return true;
    }

    public boolean hardDeleteDoctor(String doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            return false;
        }
        doctorRepository.deleteById(doctorId);
        return true;
    }

    private DoctorDTO convertToDoctorDTO(Doctor doctor) {
        DoctorDTO dto = new DoctorDTO();
        dto.setId(doctor.getId());
        dto.setEmail(doctor.getEmail());
        dto.setFullName(doctor.getFullName());
        dto.setPhoneNumber(doctor.getPhoneNumber());
        dto.setIdentityNumber(doctor.getIdentityNumber());
        dto.setGender(doctor.getGender());
        dto.setDateOfBirth(doctor.getDateOfBirth());
        dto.setAddress(doctor.getAddress());
        dto.setAvatarPath(doctor.getAvatarPath());
        dto.setStatus(doctor.getStatus());
        return dto;
    }
}
package com.example.SmartHospital.helper;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;
import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Admin;
import com.example.SmartHospital.model.Department;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.repository.DepartmentRepository;
import com.example.SmartHospital.repository.UserRepository;

@Component 
@lombok.RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner{
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    public void run(String... args) throws Exception {
        createDepartmentListIfNotExists();
        createAdminIfNotExists();
        createBulkDoctors(5);
        createBulkPatients(50);
    }

    private void createAdminIfNotExists() {
        if (userRepository.existsByEmail("admin@hospital.com")) return;
    
        Admin admin = new Admin();
        admin.setFullName("System Admin");
        admin.setEmail("admin@hospital.com");
        admin.setPhoneNumber("0900000000");
        admin.setIdentityNumber("ADMIN0001");
        admin.setAddress("321 Hospital Street");
        admin.setDateOfBirth(LocalDate.parse("1980-01-01")); 
        admin.setGender(GenderType.MALE);
        admin.setRole(RoleType.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
      
        admin.setHashedPassword(passwordEncoder.encode("admin123"));

        userRepository.save(admin);
    }
    private void createBulkDoctors(int count) {
        List<Department> departments = departmentRepository.findAll();
        if (departments.isEmpty()) return;

        for (int deptIndex = 0; deptIndex < departments.size(); deptIndex++) {
            Department department = departments.get(deptIndex);
            String departmentSlug = department.getName().toLowerCase().replaceAll("[^a-z0-9]+", "");

            for (int i = 1; i <= count; i++) {
                String doctorSuffix = (deptIndex + 1) + "" + String.format("%02d", i);
                String email = "doctor" + departmentSlug + doctorSuffix + "@hospital.com";

                if (userRepository.existsByEmail(email)) continue;

                Doctor doctor = new Doctor();
                doctor.setFullName("Doctor " + department.getName() + " " + i);
                doctor.setEmail(email);
                doctor.setPhoneNumber("091" + String.format("%07d", deptIndex * 100 + i));
                doctor.setIdentityNumber("DOC" + String.format("%04d", deptIndex * 100 + i));
                doctor.setDateOfBirth(LocalDate.of(1970 + ((deptIndex + i) % 20), 1 + ((i - 1) % 12), 1 + ((deptIndex + i) % 20)));
                doctor.setGender(i % 2 == 0 ? GenderType.MALE : GenderType.FEMALE);
                doctor.setRole(RoleType.DOCTOR);
                doctor.setStatus(UserStatus.ACTIVE);
                doctor.setAddress("Doctor Address " + department.getName() + " " + i);
                doctor.setHashedPassword(passwordEncoder.encode("Doctor123@"));

                doctor.setWorkingHours("07:00-12:00,13:00-17:00");
                doctor.setAvailabilityStatus("AVAILABLE");
                doctor.setSpecialization(department.getName());
                doctor.setDepartment(department);

                userRepository.save(doctor);
            }
        }
    }

    private void createBulkPatients(int count) {
        for (int i = 1; i <= count; i++) {
            String email = "patient" + i + "@hospital.com";

            if (userRepository.existsByEmail(email)) continue;

            Patient patient = new Patient();
            patient.setFullName("Patient " + i);
            patient.setEmail(email);
            patient.setPhoneNumber("0920000" + String.format("%03d", i));
            patient.setIdentityNumber("PAT" + String.format("%04d", i));
            patient.setDateOfBirth(LocalDate.of(1990 + (i % 20), 1 + (i % 12), 1 + (i % 20)));
            patient.setGender(i % 2 == 0 ? GenderType.MALE : GenderType.FEMALE);
            patient.setRole(RoleType.PATIENT);
            patient.setStatus(UserStatus.ACTIVE);
            patient.setAddress("Patient Address " + i);
            patient.setHashedPassword(passwordEncoder.encode("Patient123@"));

            patient.setInsuranceNumber("INS" + String.format("%05d", i));
            patient.setInsuranceProvider("Insurance Co " + ((i % 5) + 1));

            userRepository.save(patient);
        }
    }

    private void createDepartmentListIfNotExists() {
        String[] defaultDepartments = {"General", "Cardiology", "Neurology", "Pediatrics", "Orthopedics",
            "Dermatology", "Psychiatry", "Oncology", "Gynecology", "Emergency",
            "Radiology", "Urology", "Gastroenterology", "Ophthalmology", "Otolaryngology",
            "Anesthesiology", "Pathology", "Nephrology", "Endocrinology", "Hematology",
            "Rheumatology", "Pulmonology", "Infectious Diseases", "Physical Therapy", "Nutrition"
        };
        for (String deptName : defaultDepartments) {
            if (!departmentRepository.findByName(deptName).isPresent()) {
                Department department = new Department();
                department.setName(deptName);
                departmentRepository.save(department);
            }
        }
    }


}
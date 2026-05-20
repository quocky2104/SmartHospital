package com.example.SmartHospital.helper;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.SmartHospital.enums.GenderType;
import com.example.SmartHospital.enums.RoleType;
import com.example.SmartHospital.enums.UserStatus;
import com.example.SmartHospital.model.Admin;
import com.example.SmartHospital.model.Department;
import com.example.SmartHospital.model.Doctor;
import com.example.SmartHospital.model.EmergencyContact;
import com.example.SmartHospital.model.MedicalRecord;
import com.example.SmartHospital.model.Patient;
import com.example.SmartHospital.repository.DepartmentRepository;
import com.example.SmartHospital.repository.DoctorRepository;
import com.example.SmartHospital.repository.MedicalRecordRepository;
import com.example.SmartHospital.repository.PatientRepository;
import com.example.SmartHospital.repository.UserRepository;

@Component 
@lombok.RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner{
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    public void run(String... args) throws Exception {
        createDepartmentListIfNotExists();
        createAdminIfNotExists();
        createBulkDoctors(5);
        createBulkPatients(50);
        createSampleMedicalRecords();
    }

    private void createAdminIfNotExists() {
        if (userRepository.existsByEmail("admin@hospital.com")) return;
    
        Admin admin = new Admin();
        admin.setFullName("System Admin");
        admin.setEmail("admin@hospital.com");
        admin.setPhoneNumber("0900000000");
        admin.setIdentityNumber("ADMIN0001");
        admin.setAddress("321 Hospital Street");
        admin.setCity("Ho Chi Minh City");
        admin.setZipCode("700000");
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
                String email = "doctor" + departmentSlug + String.format("%02d", i) + "@hospital.com";

                if (userRepository.existsByEmail(email)) continue;

                Doctor doctor = new Doctor();
                String firstName = "Doctor";
                String lastName = department.getName() + " " + i;
                doctor.setFirstName(firstName);
                doctor.setLastName(lastName);
                doctor.setFullName(firstName + " " + lastName);
                doctor.setEmail(email);
                doctor.setPhoneNumber("091" + String.format("%07d", deptIndex * 100 + i));
                doctor.setIdentityNumber("DOC" + String.format("%04d", deptIndex * 100 + i));
                doctor.setDateOfBirth(LocalDate.of(1970 + ((deptIndex + i) % 20), 1 + ((i - 1) % 12), 1 + ((deptIndex + i) % 20)));
                doctor.setGender(i % 2 == 0 ? GenderType.MALE : GenderType.FEMALE);
                doctor.setRole(RoleType.DOCTOR);
                doctor.setStatus(UserStatus.ACTIVE);
                doctor.setAddress("Doctor Address " + department.getName() + " " + i);
                doctor.setCity("Ho Chi Minh City");
                doctor.setZipCode("700000");
                doctor.setHashedPassword(passwordEncoder.encode("Doctor123@"));

                doctor.setWorkingHours("07:00-12:00,13:00-18:00");
                doctor.setAvailabilityStatus("AVAILABLE");
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
            String firstName = "Patient";
            String lastName = String.valueOf(i);
            patient.setFirstName(firstName);
            patient.setLastName(lastName);
            patient.setFullName(firstName + " " + lastName);
            patient.setEmail(email);
            patient.setPhoneNumber("0920000" + String.format("%03d", i));
            patient.setIdentityNumber("PAT" + String.format("%04d", i));
            patient.setDateOfBirth(LocalDate.of(1990 + (i % 20), 1 + (i % 12), 1 + (i % 20)));
            patient.setGender(i % 2 == 0 ? GenderType.MALE : GenderType.FEMALE);
            patient.setRole(RoleType.PATIENT);
            patient.setStatus(UserStatus.ACTIVE);
            patient.setAddress("Patient Address " + i);
            patient.setCity("Ho Chi Minh City");
            patient.setZipCode("700000");
            patient.setHashedPassword(passwordEncoder.encode("Patient123@"));

            patient.setInsuranceNumber("INS" + String.format("%05d", i));
            patient.setInsuranceId("INS-ID-" + String.format("%05d", i));
            patient.setInsuranceProvider("Insurance Co " + ((i % 5) + 1));
            patient.setBloodType(i % 4 == 0 ? "A" : i % 4 == 1 ? "B" : i % 4 == 2 ? "AB" : "O");
            patient.setEmergencyContacts(List.of(
                new EmergencyContact("093" + String.format("%07d", i), "Parent"),
                new EmergencyContact("094" + String.format("%07d", i), "Sibling")
            ));

            userRepository.save(patient);
        }
    }

    private void createDepartmentListIfNotExists() {
        String[] defaultDepartments = {"General", "Cardiology", "Neurology", "Pediatrics", "Orthopedics",
            "Dermatology", "Psychiatry", "Oncology", "Gynecology",
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

    private void createSampleMedicalRecords() {
        if (medicalRecordRepository.count() > 0) {
            return;
        }

        List<Patient> patients = patientRepository.findAll();
        List<Doctor> doctors = doctorRepository.findAll();
        if (patients.isEmpty() || doctors.isEmpty()) {
            return;
        }

        Patient patient = patients.get(0);
        Doctor doctor = doctors.get(0);

        MedicalRecord visit = new MedicalRecord();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setRecordType("visit");
        visit.setRecordTitle("Initial Consultation");
        visit.setSummary("Routine consultation with normal vitals and lifestyle guidance.");
        visit.setTreatmentNotes("Routine consultation with normal vitals and lifestyle guidance.");
        visit.setDiagnoses(List.of("Routine checkup"));
        visit.setAttachments(List.of());
        visit.setIsDeleted(false);
        medicalRecordRepository.save(visit);

        MedicalRecord lab = new MedicalRecord();
        lab.setPatient(patient);
        lab.setDoctor(doctor);
        lab.setRecordType("lab_result");
        lab.setRecordTitle("Complete Blood Count");
        lab.setSummary("CBC within normal range.");
        lab.setTreatmentNotes("CBC within normal range.");
        lab.setLabName("Complete Blood Count");
        lab.setResultStatus("Normal");
        lab.setAttachments(List.of());
        lab.setDiagnoses(List.of("Normal CBC"));
        lab.setIsDeleted(false);
        medicalRecordRepository.save(lab);
    }


}
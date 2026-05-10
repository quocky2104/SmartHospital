package com.example.SmartHospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class SmartHospitalApplication {

	public static void main(String[] args) {
        loadEnvFile();
        SpringApplication.run(SmartHospitalApplication.class, args);
    }

    private static void loadEnvFile() {
        try {
            Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();
            dotenv.entries().forEach(entry -> 
                System.setProperty(entry.getKey(), entry.getValue())
            );
        } catch (Exception e) {
            System.err.println("Warning: Failed to load .env file - " + e.getMessage());
        }
    }

}

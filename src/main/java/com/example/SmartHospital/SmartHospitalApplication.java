package com.example.SmartHospital;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
            File envFile = new File(".env");
            if (envFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(envFile));
                props.forEach((k, v) -> System.setProperty(k.toString(), v.toString()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load .env", e);
        }
    }

}

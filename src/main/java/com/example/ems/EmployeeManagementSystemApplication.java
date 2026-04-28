package com.example.ems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EmployeeManagementSystemApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true"); // ensures AWT runs without display
        SpringApplication.run(EmployeeManagementSystemApplication.class, args);
    }

}

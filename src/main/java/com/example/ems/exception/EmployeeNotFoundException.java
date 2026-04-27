package com.example.ems.exception;

// Runtime exception thrown when an employee ID doesn't exist
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Integer id) {
        super("Employee not found with ID: " + id);
    }

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
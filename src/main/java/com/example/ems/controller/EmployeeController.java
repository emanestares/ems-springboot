package com.example.ems.controller;

import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Employee;
import com.example.ems.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Employee> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department) {

        if (search != null)     return service.searchByName(search);
        if (department != null) return service.findByDepartment(department);
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Employee> create(@Valid @RequestBody Employee emp) {
        Employee saved = service.save(emp);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public Employee update(@PathVariable Integer id,
                           @Valid @RequestBody Employee emp) {
        return service.update(id, emp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/employees/{id}/toggle-active
    @PatchMapping("/{id}/toggle-active")
    public Employee toggleActive(@PathVariable Integer id) {
        return service.toggleActive(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(required = false, defaultValue = "active") String filter) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEmployees",  service.findAll().size());
        stats.put("activeEmployees", service.countActive());
        stats.put("filter", filter);

        switch (filter) {
            case "all" -> {
                stats.put("averageSalary", Math.round(service.averageSalary()      * 100.0) / 100.0);
                stats.put("averageAge",    Math.round(service.averageAge()          * 10.0)  / 10.0);
                stats.put("byDepartment",  service.groupedByDepartment());
            }
            case "inactive" -> {
                stats.put("averageSalary", Math.round(service.averageSalaryInactive() * 100.0) / 100.0);
                stats.put("averageAge",    Math.round(service.averageAgeInactive()     * 10.0)  / 10.0);
                stats.put("byDepartment",  service.groupedByDepartmentInactive());
            }
            default -> { // "active"
                stats.put("averageSalary", Math.round(service.averageSalaryActive() * 100.0) / 100.0);
                stats.put("averageAge",    Math.round(service.averageAgeActive()     * 10.0)  / 10.0);
                stats.put("byDepartment",  service.groupedByDepartmentActive());
            }
        }
        return stats;
    }

    @GetMapping("/report/by-department")
    public List<Employee> reportByDepartment() {
        return service.reportByDepartment();
    }

    @GetMapping("/report/by-age")
    public List<Employee> reportByAge() {
        return service.reportByAge();
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String,String>> handleNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> handleBadInput(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
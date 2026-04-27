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
@CrossOrigin(origins = "*") // allow requests from your HTML file
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    // GET /api/employees          — list all
    // GET /api/employees?search=x — search by name
    @GetMapping
    public List<Employee> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department) {

        if (search != null)     return service.searchByName(search);
        if (department != null) return service.findByDepartment(department);
        return service.findAll();
    }

    // GET /api/employees/{id}
    @GetMapping("/{id}")
    public Employee getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    // POST /api/employees
    @PostMapping
    public ResponseEntity<Employee> create(@Valid @RequestBody Employee emp) {
        Employee saved = service.save(emp);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // PUT /api/employees/{id}
    @PutMapping("/{id}")
    public Employee update(@PathVariable Integer id,
                           @Valid @RequestBody Employee emp) {
        return service.update(id, emp);
    }

    // DELETE /api/employees/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/employees/stats
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEmployees", service.findAll().size());
        stats.put("averageSalary",  Math.round(service.averageSalary() * 100.0) / 100.0);
        stats.put("averageAge",     Math.round(service.averageAge()    * 10.0)  / 10.0);
        stats.put("byDepartment",   service.groupedByDepartment());
        return stats;
    }

    // GET /api/employees/report/by-department
    @GetMapping("/report/by-department")
    public List<Employee> reportByDepartment() {
        return service.reportByDepartment();
    }

    // GET /api/employees/report/by-age
    @GetMapping("/report/by-age")
    public List<Employee> reportByAge() {
        return service.reportByAge();
    }

    // Global error handler for this controller
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
package com.example.ems.controller;

import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Employee;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeService      service;
    private final DepartmentRepository deptRepo;

    @Value("${pagination.default-size:5}")
    private int defaultPageSize;

    public EmployeeController(EmployeeService service,
                              DepartmentRepository deptRepo) {
        this.service  = service;
        this.deptRepo = deptRepo;
    }

    /**
     * GET /api/employees?page=0&size=5&search=&department=
     * Returns a paginated response with metadata.
     */
    @GetMapping
    public Map<String, Object> getAll(
            @RequestParam(required = false)              String  search,
            @RequestParam(required = false)              String  department,
            @RequestParam(defaultValue = "0")            int     page,
            @RequestParam(required = false)              Integer size) {

        int pageSize = (size != null) ? size : defaultPageSize;
        Page<Employee> result;

        if (search != null && !search.isBlank()) {
            result = service.searchByNamePaged(search, page, pageSize);
        } else if (department != null && !department.isBlank()) {
            result = service.findByDepartmentPaged(department, page, pageSize);
        } else {
            result = service.findAllPaged(page, pageSize);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content",       result.getContent());
        response.put("currentPage",   result.getNumber());
        response.put("totalPages",    result.getTotalPages());
        response.put("totalElements", result.getTotalElements());
        response.put("pageSize",      result.getSize());
        return response;
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<Employee> create(@Valid @RequestBody EmployeeRequest req) {
        Employee emp = buildEmployee(req, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(emp));
    }

    @PutMapping("/{id}")
    public Employee update(@PathVariable Integer id,
                           @Valid @RequestBody EmployeeRequest req) {
        Employee emp = buildEmployee(req, id);
        return service.update(id, emp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public Employee toggleActive(@PathVariable Integer id) {
        return service.toggleActive(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam(defaultValue = "active") String filter) {
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
            default -> {
                stats.put("averageSalary", Math.round(service.averageSalaryActive() * 100.0) / 100.0);
                stats.put("averageAge",    Math.round(service.averageAgeActive()     * 10.0)  / 10.0);
                stats.put("byDepartment",  service.groupedByDepartmentActive());
            }
        }
        return stats;
    }

    @GetMapping("/report/by-department")
    public List<Employee> reportByDepartment() { return service.reportByDepartment(); }

    @GetMapping("/report/by-age")
    public List<Employee> reportByAge() { return service.reportByAge(); }

    // ── Request DTO ───────────────────────────────────────────────────────

    /**
     * Accepts departmentId (Integer) instead of a full department string.
     */
    public static class EmployeeRequest {
        public String  firstname;
        public String  lastname;
        public String  birthday;      // ISO date string yyyy-MM-dd
        public Integer departmentId;
        public Double  salary;
    }

    private Employee buildEmployee(EmployeeRequest req, Integer existingId) {
        Employee emp = existingId != null ? service.findById(existingId) : new Employee();
        emp.setFirstname(req.firstname);
        emp.setLastname(req.lastname);
        if (req.birthday != null && !req.birthday.isBlank()) {
            emp.setBirthday(java.time.LocalDate.parse(req.birthday));
        } else {
            emp.setBirthday(null);
        }
        emp.setSalary(req.salary);
        if (req.departmentId != null) {
            deptRepo.findById(req.departmentId).ifPresent(emp::setDepartment);
        }
        return emp;
    }

    // ── Exception handlers ────────────────────────────────────────────────

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadInput(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}

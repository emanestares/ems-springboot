package com.example.ems.service;

import com.example.ems.model.Employee;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface EmployeeService {

    // ── CRUD ─────────────────────────────────────────────────────────────
    Employee       save(Employee employee);
    Employee       findById(Integer id);
    Employee       update(Integer id, Employee updated);
    void           delete(Integer id);
    Employee       toggleActive(Integer id);

    // ── Paginated queries (5 per page) ────────────────────────────────────
    Page<Employee> findAllPaged(int page, int size);
    Page<Employee> searchByNamePaged(String keyword, int page, int size);
    Page<Employee> findByDepartmentPaged(String department, int page, int size);

    // ── Unpaginated (for reports / stats) ────────────────────────────────
    List<Employee> findAll();
    List<Employee> searchByName(String keyword);
    List<Employee> findByDepartment(String department);

    // ── Reports ───────────────────────────────────────────────────────────
    List<Employee> reportByDepartment();
    List<Employee> reportByAge();
    List<Employee> reportByAgeActive();
    List<Employee> reportByAgeInactive();
    List<Employee> reportByDepartmentActive();
    List<Employee> reportByDepartmentInactive();

    // ── Aggregates ────────────────────────────────────────────────────────
    double averageSalary();
    double averageSalaryActive();
    double averageSalaryInactive();
    double averageAge();
    double averageAgeActive();
    double averageAgeInactive();
    long   countActive();
    Map<String, List<Employee>> groupedByDepartment();
    Map<String, List<Employee>> groupedByDepartmentActive();
    Map<String, List<Employee>> groupedByDepartmentInactive();
}

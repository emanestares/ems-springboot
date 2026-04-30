package com.example.ems.service;

import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Employee;
import com.example.ems.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository repo;

    public EmployeeServiceImpl(EmployeeRepository repo) {
        this.repo = repo;
    }

    @Override
    public Employee save(Employee emp) {
        validate(emp);
        if (emp.getIsActive() == null) emp.setIsActive(true);
        return repo.save(emp);
    }

    @Override
    public Employee findById(Integer id) {
        return repo.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Override
    public List<Employee> findAll() {
        return new ArrayList<>(repo.findAll());
    }

    @Override
    public Employee update(Integer id, Employee updated) {
        Employee existing = findById(id);
        validate(updated);

        existing.setFirstname(updated.getFirstname());
        existing.setLastname(updated.getLastname());
        existing.setBirthday(updated.getBirthday());
        existing.setDepartment(updated.getDepartment());
        existing.setSalary(updated.getSalary());
        return repo.save(existing);
    }

    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id))
            throw new EmployeeNotFoundException(id);
        repo.deleteById(id);
    }

    @Override
    public Employee toggleActive(Integer id) {
        Employee emp = findById(id);
        emp.setIsActive(!emp.getIsActive());
        return repo.save(emp);
    }

    @Override
    public List<Employee> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank())
            return findAll();
        return repo.searchByName(keyword.trim());
    }

    @Override
    public List<Employee> findByDepartment(String dept) {
        return repo.findByDepartmentIgnoreCase(dept);
    }

    @Override
    public List<Employee> reportByDepartment() {
        return repo.findAllByOrderByDepartmentAsc();
    }

    @Override
    public List<Employee> reportByAge() {
        return repo.findAllByOrderByBirthdayAsc();
    }

    @Override
    public double averageSalary() {
        Double avg = repo.findAverageSalary();
        return avg != null ? avg : 0.0;
    }

    @Override
    public double averageAge() {
        Double avg = repo.findAverageAge();
        return avg != null ? avg : 0.0;
    }

    @Override
    public long countActive() {
        return repo.countByIsActiveTrue();
    }

    @Override
    public Map<String, List<Employee>> groupedByDepartment() {
        return repo.findAll().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getDepartment() != null ? e.getDepartment() : "Unassigned"
                ));
    }

    private void validate(Employee e) {
        if (e.getLastname() == null || e.getLastname().isBlank())
            throw new IllegalArgumentException("Last name cannot be empty");
        if (e.getSalary() != null && e.getSalary() < 0)
            throw new IllegalArgumentException("Salary cannot be negative");
        if (e.getAge() > 100 || (e.getBirthday() != null && e.getAge() < 16))
            throw new IllegalArgumentException("Age must be between 16 and 100");
    }
}

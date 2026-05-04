package com.example.ems.service;

import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Department;
import com.example.ems.model.Employee;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository   repo;
    private final DepartmentRepository deptRepo;

    // ── Externalized messages ─────────────────────────────────────────────
    @Value("${msg.emp.lastname.required}")
    private String msgLastnameRequired;

    @Value("${msg.emp.salary.negative}")
    private String msgSalaryNegative;

    @Value("${msg.emp.age.invalid}")
    private String msgAgeInvalid;

    @Value("${msg.emp.dept.required}")
    private String msgDeptRequired;

    @Value("${msg.emp.dept.notfound}")
    private String msgDeptNotFound;

    // ── Pagination default ────────────────────────────────────────────────
    @Value("${pagination.default-size:5}")
    private int defaultPageSize;

    public EmployeeServiceImpl(EmployeeRepository repo,
                               DepartmentRepository deptRepo) {
        this.repo     = repo;
        this.deptRepo = deptRepo;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────

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
        if (!repo.existsById(id)) throw new EmployeeNotFoundException(id);
        repo.deleteById(id);
    }

    @Override
    public Employee toggleActive(Integer id) {
        Employee emp = findById(id);
        emp.setIsActive(!emp.getIsActive());
        return repo.save(emp);
    }

    // ── Paginated ─────────────────────────────────────────────────────────

    @Override
    public Page<Employee> findAllPaged(int page, int size) {
        return repo.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
    }

    @Override
    public Page<Employee> searchByNamePaged(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) return findAllPaged(page, size);
        return repo.searchByName(keyword.trim(),
                PageRequest.of(page, size, Sort.by("lastname").ascending()));
    }

    @Override
    public Page<Employee> findByDepartmentPaged(String department, int page, int size) {
        return repo.findByDepartmentName(department,
                PageRequest.of(page, size, Sort.by("lastname").ascending()));
    }

    // ── Unpaginated ───────────────────────────────────────────────────────

    @Override
    public List<Employee> findAll() {
        return new ArrayList<>(repo.findAll());
    }

    @Override
    public List<Employee> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        return repo.searchByName(keyword.trim(), Pageable.unpaged()).getContent();
    }

    @Override
    public List<Employee> findByDepartment(String dept) {
        return repo.findByDepartmentName(dept, Pageable.unpaged()).getContent();
    }

    // ── Reports ───────────────────────────────────────────────────────────

    @Override public List<Employee> reportByDepartment()         { return repo.findAllByOrderByDepartmentAsc(); }
    @Override public List<Employee> reportByAge()                { return repo.findAllByOrderByBirthdayAsc(); }
    @Override public List<Employee> reportByAgeActive()          { return repo.findAllByIsActiveTrueOrderByBirthdayAsc(); }
    @Override public List<Employee> reportByDepartmentActive()   { return repo.findAllByIsActiveTrueOrderByDepartmentAsc(); }
    @Override public List<Employee> reportByDepartmentInactive() { return repo.findAllByIsActiveFalseOrderByDepartmentAsc(); }
    @Override public List<Employee> reportByAgeInactive()        { return repo.findAllByIsActiveFalseOrderByBirthdayAsc(); }

    // ── Aggregates ────────────────────────────────────────────────────────

    @Override public double averageSalary()         { Double v = repo.findAverageSalary();         return v != null ? v : 0.0; }
    @Override public double averageSalaryActive()   { Double v = repo.findAverageSalaryActive();   return v != null ? v : 0.0; }
    @Override public double averageSalaryInactive() { Double v = repo.findAverageSalaryInactive(); return v != null ? v : 0.0; }
    @Override public double averageAge()            { Double v = repo.findAverageAge();            return v != null ? v : 0.0; }
    @Override public double averageAgeActive()      { Double v = repo.findAverageAgeActive();      return v != null ? v : 0.0; }
    @Override public double averageAgeInactive()    { Double v = repo.findAverageAgeInactive();    return v != null ? v : 0.0; }
    @Override public long   countActive()           { return repo.countByIsActiveTrue(); }

    @Override
    public Map<String, List<Employee>> groupedByDepartment() {
        return group(findAll());
    }

    @Override
    public Map<String, List<Employee>> groupedByDepartmentActive() {
        return group(repo.findAllByIsActiveTrueOrderByDepartmentAsc());
    }

    @Override
    public Map<String, List<Employee>> groupedByDepartmentInactive() {
        return group(repo.findAllByIsActiveFalseOrderByDepartmentAsc());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Map<String, List<Employee>> group(List<Employee> list) {
        return list.stream().collect(Collectors.groupingBy(
                e -> e.getDepartment() != null ? e.getDepartment().getName() : "Unassigned"
        ));
    }

    private void validate(Employee e) {
        if (e.getLastname() == null || e.getLastname().isBlank())
            throw new IllegalArgumentException(msgLastnameRequired);
        if (e.getDepartment() == null)
            throw new IllegalArgumentException(msgDeptRequired);
        // Verify department exists in DB
        if (e.getDepartment().getId() != null) {
            deptRepo.findById(e.getDepartment().getId())
                    .orElseThrow(() -> new IllegalArgumentException(msgDeptNotFound));
        }
        if (e.getSalary() != null && e.getSalary() < 0)
            throw new IllegalArgumentException(msgSalaryNegative);
        if (e.getBirthday() != null && (e.getAge() > 100 || e.getAge() < 16))
            throw new IllegalArgumentException(msgAgeInvalid);
    }
}

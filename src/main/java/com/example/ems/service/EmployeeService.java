package com.example.ems.service;

import com.example.ems.model.Employee;
import java.util.List;
import java.util.Map;

public interface EmployeeService {
    Employee      save(Employee employee);
    Employee      findById(Integer id);
    List<Employee> findAll();
    Employee      update(Integer id, Employee updated);
    void          delete(Integer id);
    Employee      toggleActive(Integer id);

    List<Employee> searchByName(String keyword);
    List<Employee> findByDepartment(String department);

    // Reports
    List<Employee> reportByDepartment();
    List<Employee> reportByAge();
    List<Employee> reportByAgeActive();
    List<Employee> reportByAgeInactive();
    List<Employee> reportByDepartmentInactive();
    double averageSalaryInactive();
    double averageAgeInactive();
    Map<String, List<Employee>> groupedByDepartmentActive();
    Map<String, List<Employee>> groupedByDepartmentInactive();
    List<Employee> reportByDepartmentActive();

    // Calculations
    double averageSalary();
    double averageSalaryActive();
    double averageAge();
    double averageAgeActive();
    long   countActive();
    Map<String, List<Employee>> groupedByDepartment();


}
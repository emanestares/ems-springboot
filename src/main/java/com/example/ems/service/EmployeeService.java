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

    List<Employee> searchByName(String keyword);
    List<Employee> findByDepartment(String department);

    // Reports
    List<Employee> reportByDepartment();
    List<Employee> reportByAge();

    // Calculations
    double averageSalary();
    double averageAge();
    Map<String, List<Employee>> groupedByDepartment();
}
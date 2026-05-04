package com.example.ems.service;

import com.example.ems.model.Department;
import java.util.List;

public interface DepartmentService {
    List<Department> findAll();
    Department findById(Integer id);
    Department save(Department department);
    Department update(Integer id, Department department);
    void delete(Integer id);
}

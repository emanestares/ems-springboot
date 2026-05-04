package com.example.ems.service;

import com.example.ems.model.Department;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository deptRepo;
    private final EmployeeRepository   empRepo;

    // Messages externalized from messages.properties
    @Value("${msg.dept.notfound}")
    private String msgNotFound;

    @Value("${msg.dept.name.required}")
    private String msgNameRequired;

    @Value("${msg.dept.name.duplicate}")
    private String msgNameDuplicate;

    @Value("${msg.dept.inuse}")
    private String msgInUse;

    public DepartmentServiceImpl(DepartmentRepository deptRepo,
                                 EmployeeRepository empRepo) {
        this.deptRepo = deptRepo;
        this.empRepo  = empRepo;
    }

    @Override
    public List<Department> findAll() {
        return deptRepo.findAll();
    }

    @Override
    public Department findById(Integer id) {
        return deptRepo.findById(id)
                .orElseThrow(() -> new RuntimeException(msgNotFound + id));
    }

    @Override
    public Department save(Department department) {
        validate(department, null);
        return deptRepo.save(department);
    }

    @Override
    public Department update(Integer id, Department updated) {
        Department existing = findById(id);
        validate(updated, id);
        existing.setName(updated.getName());
        return deptRepo.save(existing);
    }

    @Override
    public void delete(Integer id) {
        Department dept = findById(id);
        // Use a targeted count query instead of loading all employees
        if (deptRepo.countEmployeesByDepartmentId(id) > 0) {
            throw new IllegalStateException(msgInUse);
        }
        deptRepo.delete(dept);
    }

    private void validate(Department d, Integer excludeId) {
        if (d.getName() == null || d.getName().isBlank()) {
            throw new IllegalArgumentException(msgNameRequired);
        }
        deptRepo.findByNameIgnoreCase(d.getName().trim()).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException(msgNameDuplicate);
            }
        });
    }
}

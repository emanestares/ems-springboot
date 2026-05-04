package com.example.ems.repository;

import com.example.ems.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    Optional<Department> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    /** Count employees assigned to a given department id — avoids loading all employees */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :deptId")
    long countEmployeesByDepartmentId(@Param("deptId") Integer deptId);
}

package com.example.ems.repository;

import com.example.ems.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findByDepartmentIgnoreCase(String department);

    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(e.firstname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.lastname)  LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Employee> searchByName(String keyword);

    @Query("SELECT AVG(e.salary) FROM Employee e")
    Double findAverageSalary();

    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(e.birthday)) FROM Employee e " +
            "WHERE e.birthday IS NOT NULL")
    Double findAverageAge();

    List<Employee> findAllByOrderByDepartmentAsc();

    List<Employee> findAllByOrderByBirthdayAsc();

    long countByIsActiveTrue();
}

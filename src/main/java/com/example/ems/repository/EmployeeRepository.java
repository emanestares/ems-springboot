package com.example.ems.repository;

import com.example.ems.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // Spring auto-generates the SQL from the method name!
    // SELECT * FROM employee WHERE department = ?
    List<Employee> findByDepartmentIgnoreCase(String department);

    // Custom JPQL for search by name
    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(e.firstname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.lastname)  LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Employee> searchByName(String keyword);

    // Average salary — returns a single double
    @Query("SELECT AVG(e.salary) FROM Employee e")
    Double findAverageSalary();

    // Average age via birthday
    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(e.birthday)) FROM Employee e " +
            "WHERE e.birthday IS NOT NULL")
    Double findAverageAge();

    // List by department alphabetically
    List<Employee> findAllByOrderByDepartmentAsc();

    // List by birthday ascending (youngest last)
    List<Employee> findAllByOrderByBirthdayAsc();
}
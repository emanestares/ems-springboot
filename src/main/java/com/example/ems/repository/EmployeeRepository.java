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

    @Query("SELECT AVG(e.salary) FROM Employee e WHERE e.isActive = true")
    Double findAverageSalaryActive();

    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(e.birthday) - " +
            "CASE WHEN (MONTH(e.birthday) > MONTH(CURRENT_DATE) OR " +
            "(MONTH(e.birthday) = MONTH(CURRENT_DATE) AND DAY(e.birthday) > DAY(CURRENT_DATE))) " +
            "THEN 1 ELSE 0 END) " +
            "FROM Employee e WHERE e.birthday IS NOT NULL")
    Double findAverageAge();

    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(e.birthday) - " +
            "CASE WHEN (MONTH(e.birthday) > MONTH(CURRENT_DATE) OR " +
            "(MONTH(e.birthday) = MONTH(CURRENT_DATE) AND DAY(e.birthday) > DAY(CURRENT_DATE))) " +
            "THEN 1 ELSE 0 END) " +
            "FROM Employee e WHERE e.birthday IS NOT NULL AND e.isActive = true")
    Double findAverageAgeActive();

    List<Employee> findAllByOrderByDepartmentAsc();

    List<Employee> findAllByOrderByBirthdayAsc();

    long countByIsActiveTrue();

    List<Employee> findAllByIsActiveTrueOrderByBirthdayAsc();

    @Query("SELECT AVG(e.salary) FROM Employee e WHERE e.isActive = false")
    Double findAverageSalaryInactive();

    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(e.birthday) - " +
            "CASE WHEN (MONTH(e.birthday) > MONTH(CURRENT_DATE) OR " +
            "(MONTH(e.birthday) = MONTH(CURRENT_DATE) AND DAY(e.birthday) > DAY(CURRENT_DATE))) " +
            "THEN 1 ELSE 0 END) " +
            "FROM Employee e WHERE e.birthday IS NOT NULL AND e.isActive = false")
    Double findAverageAgeInactive();

    List<Employee> findAllByIsActiveFalseOrderByBirthdayAsc();

    List<Employee> findAllByIsActiveFalseOrderByDepartmentAsc();

    List<Employee> findAllByIsActiveTrueOrderByDepartmentAsc();
}
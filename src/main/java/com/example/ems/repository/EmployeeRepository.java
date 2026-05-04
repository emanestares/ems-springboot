package com.example.ems.repository;

import com.example.ems.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // ── Paginated list (all / active / inactive) ──────────────────────────
    Page<Employee> findAll(Pageable pageable);
    Page<Employee> findByIsActiveTrue(Pageable pageable);
    Page<Employee> findByIsActiveFalse(Pageable pageable);

    // ── Search by name (paginated) ────────────────────────────────────────
    @Query("SELECT e FROM Employee e WHERE " +
            "LOWER(e.firstname) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
            "LOWER(e.lastname)  LIKE LOWER(CONCAT('%', :kw, '%'))")
    Page<Employee> searchByName(@Param("kw") String keyword, Pageable pageable);

    // ── Filter by department (paginated) ──────────────────────────────────
    @Query("SELECT e FROM Employee e WHERE LOWER(e.department.name) = LOWER(:name)")
    Page<Employee> findByDepartmentName(@Param("name") String name, Pageable pageable);

    // ── Unpaginated (for reports / PDF / stats) ───────────────────────────
    @Query("SELECT e FROM Employee e ORDER BY e.department.name ASC")
    List<Employee> findAllByOrderByDepartmentAsc();

    List<Employee> findAllByOrderByBirthdayAsc();
    List<Employee> findAllByIsActiveTrueOrderByBirthdayAsc();

    @Query("SELECT e FROM Employee e WHERE e.isActive = true ORDER BY e.department.name ASC")
    List<Employee> findAllByIsActiveTrueOrderByDepartmentAsc();

    @Query("SELECT e FROM Employee e WHERE e.isActive = false ORDER BY e.department.name ASC")
    List<Employee> findAllByIsActiveFalseOrderByDepartmentAsc();

    List<Employee> findAllByIsActiveFalseOrderByBirthdayAsc();

    // ── Aggregates ────────────────────────────────────────────────────────
    long countByIsActiveTrue();

    @Query("SELECT AVG(e.salary) FROM Employee e")
    Double findAverageSalary();

    @Query("SELECT AVG(e.salary) FROM Employee e WHERE e.isActive = true")
    Double findAverageSalaryActive();

    @Query("SELECT AVG(e.salary) FROM Employee e WHERE e.isActive = false")
    Double findAverageSalaryInactive();

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

    @Query("SELECT AVG(YEAR(CURRENT_DATE) - YEAR(e.birthday) - " +
            "CASE WHEN (MONTH(e.birthday) > MONTH(CURRENT_DATE) OR " +
            "(MONTH(e.birthday) = MONTH(CURRENT_DATE) AND DAY(e.birthday) > DAY(CURRENT_DATE))) " +
            "THEN 1 ELSE 0 END) " +
            "FROM Employee e WHERE e.birthday IS NOT NULL AND e.isActive = false")
    Double findAverageAgeInactive();
}

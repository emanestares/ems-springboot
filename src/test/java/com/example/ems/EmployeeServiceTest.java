package com.example.ems;

import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Department;
import com.example.ems.model.Employee;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import com.example.ems.service.EmployeeServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock EmployeeRepository   repo;
    @Mock DepartmentRepository deptRepo;

    @InjectMocks EmployeeServiceImpl service;

    private Department dept;
    private Employee   sample;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "msgLastnameRequired", "Last name cannot be empty.");
        ReflectionTestUtils.setField(service, "msgSalaryNegative",   "Salary cannot be negative.");
        ReflectionTestUtils.setField(service, "msgAgeInvalid",       "Age must be between 16 and 100.");
        ReflectionTestUtils.setField(service, "msgDeptRequired",     "Department is required.");
        ReflectionTestUtils.setField(service, "msgDeptNotFound",     "Department not found with the given ID.");
        ReflectionTestUtils.setField(service, "defaultPageSize",     5);

        dept = new Department("Engineering");
        dept.setId(1);

        sample = new Employee("Juan", "Dela Cruz",
                LocalDate.of(1990, 6, 15), dept, 55000.0);
        sample.setId(1);
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns all employees")
    void testFindAll_returnsAll() {
        Department hr = new Department("HR");
        hr.setId(2);
        Employee e2 = new Employee("Maria", "Santos",
                LocalDate.of(1995, 3, 20), hr, 45000.0);
        e2.setId(2);

        when(repo.findAll()).thenReturn(List.of(sample, e2));

        List<Employee> result = service.findAll();

        assertEquals(2, result.size());
        verify(repo, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll returns empty list when no employees exist")
    void testFindAll_empty() {
        when(repo.findAll()).thenReturn(Collections.emptyList());

        List<Employee> result = service.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── FIND BY ID ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns the correct employee")
    void testFindById_success() {
        when(repo.findById(1)).thenReturn(Optional.of(sample));

        Employee result = service.findById(1);

        assertEquals("Juan",         result.getFirstname());
        assertEquals("Dela Cruz",    result.getLastname());
        assertEquals("Engineering",  result.getDepartment().getName());
    }

    @Test
    @DisplayName("findById throws EmployeeNotFoundException when ID not found")
    void testFindById_notFound() {
        when(repo.findById(99)).thenReturn(Optional.empty());

        EmployeeNotFoundException ex = assertThrows(
                EmployeeNotFoundException.class,
                () -> service.findById(99));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ── SAVE ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save persists a valid employee and returns saved entity")
    void testSave_valid() {
        when(deptRepo.findById(1)).thenReturn(Optional.of(dept));
        when(repo.save(sample)).thenReturn(sample);

        Employee result = service.save(sample);

        assertNotNull(result);
        assertEquals("Dela Cruz", result.getLastname());
        verify(repo, times(1)).save(sample);
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when lastname is blank")
    void testSave_blankLastname() {
        sample.setLastname("");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.save(sample));

        assertTrue(ex.getMessage().toLowerCase().contains("last name"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when lastname is null")
    void testSave_nullLastname() {
        sample.setLastname(null);

        assertThrows(IllegalArgumentException.class, () -> service.save(sample));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when department is null")
    void testSave_nullDepartment() {
        sample.setDepartment(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.save(sample));

        assertTrue(ex.getMessage().toLowerCase().contains("department"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when salary is negative")
    void testSave_negativeSalary() {
        sample.setSalary(-1.0);
        // dept check runs before salary check — stub so it passes through to salary validation
        when(deptRepo.findById(1)).thenReturn(Optional.of(dept));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.save(sample));

        assertTrue(ex.getMessage().toLowerCase().contains("salary"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("save accepts salary of zero")
    void testSave_zeroSalary() {
        sample.setSalary(0.0);
        when(deptRepo.findById(1)).thenReturn(Optional.of(dept));
        when(repo.save(sample)).thenReturn(sample);

        assertDoesNotThrow(() -> service.save(sample));
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when employee is under 16")
    void testSave_tooYoung() {
        sample.setBirthday(LocalDate.now().minusYears(15));

        assertThrows(IllegalArgumentException.class, () -> service.save(sample));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when age exceeds 100")
    void testSave_tooOld() {
        sample.setBirthday(LocalDate.now().minusYears(101));

        assertThrows(IllegalArgumentException.class, () -> service.save(sample));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("save accepts employee with null birthday")
    void testSave_nullBirthday() {
        sample.setBirthday(null);
        when(deptRepo.findById(1)).thenReturn(Optional.of(dept));
        when(repo.save(sample)).thenReturn(sample);

        assertDoesNotThrow(() -> service.save(sample));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update modifies all fields and saves")
    void testUpdate_success() {
        Department finance = new Department("Finance");
        finance.setId(2);
        Employee updated = new Employee("Pedro", "Reyes",
                LocalDate.of(1988, 1, 10), finance, 70000.0);

        when(repo.findById(1)).thenReturn(Optional.of(sample));
        when(deptRepo.findById(2)).thenReturn(Optional.of(finance));
        when(repo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.update(1, updated);

        assertEquals("Pedro",   result.getFirstname());
        assertEquals("Reyes",   result.getLastname());
        assertEquals("Finance", result.getDepartment().getName());
        assertEquals(70000.0,   result.getSalary());
        verify(repo, times(1)).save(sample);
    }

    @Test
    @DisplayName("update throws EmployeeNotFoundException when ID does not exist")
    void testUpdate_notFound() {
        Department it = new Department("IT");
        it.setId(3);
        Employee updated = new Employee("X", "Y",
                LocalDate.of(1990, 1, 1), it, 40000.0);

        when(repo.findById(99)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> service.update(99, updated));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("update throws IllegalArgumentException when updated data is invalid")
    void testUpdate_invalidData() {
        Department it = new Department("IT");
        it.setId(3);
        // blank lastname + negative salary — both invalid
        Employee badUpdate = new Employee("", "",
                LocalDate.of(1990, 1, 1), it, -500.0);

        when(repo.findById(1)).thenReturn(Optional.of(sample));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(1, badUpdate));
        verify(repo, never()).save(any());
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete removes an existing employee without throwing")
    void testDelete_success() {
        when(repo.existsById(1)).thenReturn(true);
        doNothing().when(repo).deleteById(1);

        assertDoesNotThrow(() -> service.delete(1));
        verify(repo, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("delete throws EmployeeNotFoundException when employee does not exist")
    void testDelete_notFound() {
        when(repo.existsById(99)).thenReturn(false);

        assertThrows(EmployeeNotFoundException.class,
                () -> service.delete(99));
        verify(repo, never()).deleteById(anyInt());
    }

    // ── TOGGLE ACTIVE ─────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleActive flips isActive from true to false")
    void testToggleActive_deactivates() {
        sample.setIsActive(true);
        when(repo.findById(1)).thenReturn(Optional.of(sample));
        when(repo.save(sample)).thenReturn(sample);

        Employee result = service.toggleActive(1);

        assertFalse(result.getIsActive());
        verify(repo).save(sample);
    }

    @Test
    @DisplayName("toggleActive flips isActive from false to true")
    void testToggleActive_activates() {
        sample.setIsActive(false);
        when(repo.findById(1)).thenReturn(Optional.of(sample));
        when(repo.save(sample)).thenReturn(sample);

        Employee result = service.toggleActive(1);

        assertTrue(result.getIsActive());
    }

    // ── PAGINATED FIND ────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllPaged returns a Page of employees")
    void testFindAllPaged() {
        Page<Employee> page = new PageImpl<>(List.of(sample));
        when(repo.findAll(any(Pageable.class))).thenReturn(page);

        Page<Employee> result = service.findAllPaged(0, 5);

        assertEquals(1, result.getTotalElements());
        verify(repo).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("searchByNamePaged delegates to repo.searchByName when keyword given")
    void testSearchByNamePaged_withKeyword() {
        Page<Employee> page = new PageImpl<>(List.of(sample));
        when(repo.searchByName(eq("Juan"), any(Pageable.class))).thenReturn(page);

        Page<Employee> result = service.searchByNamePaged("Juan", 0, 5);

        assertEquals(1, result.getContent().size());
        verify(repo).searchByName(eq("Juan"), any(Pageable.class));
    }

    @Test
    @DisplayName("searchByNamePaged calls findAllPaged when keyword is blank")
    void testSearchByNamePaged_blankKeyword() {
        Page<Employee> page = new PageImpl<>(List.of(sample));
        when(repo.findAll(any(Pageable.class))).thenReturn(page);

        Page<Employee> result = service.searchByNamePaged("   ", 0, 5);

        assertEquals(1, result.getTotalElements());
        verify(repo).findAll(any(Pageable.class));
        verify(repo, never()).searchByName(any(), any());
    }

    // ── SEARCH UNPAGINATED ────────────────────────────────────────────────

    @Test
    @DisplayName("searchByName delegates to repo.searchByName when keyword provided")
    void testSearchByName_withKeyword() {
        Page<Employee> page = new PageImpl<>(List.of(sample));
        when(repo.searchByName(eq("Juan"), any(Pageable.class))).thenReturn(page);

        List<Employee> result = service.searchByName("Juan");

        assertEquals(1, result.size());
        assertEquals("Juan", result.get(0).getFirstname());
    }

    @Test
    @DisplayName("searchByName returns all employees when keyword is null")
    void testSearchByName_nullKeyword() {
        when(repo.findAll()).thenReturn(List.of(sample));

        List<Employee> result = service.searchByName(null);

        assertEquals(1, result.size());
        verify(repo, times(1)).findAll();
    }

    @Test
    @DisplayName("searchByName returns all employees when keyword is blank")
    void testSearchByName_blankKeyword() {
        when(repo.findAll()).thenReturn(List.of(sample));

        List<Employee> result = service.searchByName("   ");

        assertEquals(1, result.size());
        verify(repo, times(1)).findAll();
    }

    // ── REPORTS ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("reportByDepartment returns employees ordered by department name")
    void testReportByDepartment() {
        Department hr = new Department("HR");
        hr.setId(2);
        Employee e2 = new Employee("Ana", "Lopez",
                LocalDate.of(1993, 5, 10), hr, 40000.0);

        when(repo.findAllByOrderByDepartmentAsc()).thenReturn(List.of(e2, sample));

        List<Employee> result = service.reportByDepartment();

        assertEquals(2, result.size());
        assertEquals("HR", result.get(0).getDepartment().getName());
        verify(repo).findAllByOrderByDepartmentAsc();
    }

    @Test
    @DisplayName("reportByAge returns employees ordered by birthday")
    void testReportByAge() {
        Department it = new Department("IT");
        it.setId(3);
        Employee older = new Employee("Carlos", "Gomez",
                LocalDate.of(1975, 1, 1), it, 60000.0);

        when(repo.findAllByOrderByBirthdayAsc()).thenReturn(List.of(older, sample));

        List<Employee> result = service.reportByAge();

        assertEquals(2, result.size());
        assertEquals(1975, result.get(0).getBirthday().getYear());
        verify(repo).findAllByOrderByBirthdayAsc();
    }

    // ── AGGREGATES ────────────────────────────────────────────────────────

    @Test
    @DisplayName("averageSalary returns 0 when repository returns null")
    void testAverageSalary_null() {
        when(repo.findAverageSalary()).thenReturn(null);
        assertEquals(0.0, service.averageSalary());
    }

    @Test
    @DisplayName("averageSalary returns correct value")
    void testAverageSalary_withData() {
        when(repo.findAverageSalary()).thenReturn(55000.0);
        assertEquals(55000.0, service.averageSalary());
    }

    @Test
    @DisplayName("averageAge returns 0 when repository returns null")
    void testAverageAge_null() {
        when(repo.findAverageAge()).thenReturn(null);
        assertEquals(0.0, service.averageAge());
    }

    @Test
    @DisplayName("averageAge returns correct value")
    void testAverageAge_withData() {
        when(repo.findAverageAge()).thenReturn(32.5);
        assertEquals(32.5, service.averageAge());
    }

    @Test
    @DisplayName("countActive delegates to repo.countByIsActiveTrue")
    void testCountActive() {
        when(repo.countByIsActiveTrue()).thenReturn(7L);
        assertEquals(7L, service.countActive());
    }

    // ── GROUPED BY DEPARTMENT ─────────────────────────────────────────────

    @Test
    @DisplayName("groupedByDepartment groups employees by department name")
    void testGroupedByDepartment() {
        Department hr = new Department("HR");
        hr.setId(2);
        Employee hrEmployee = new Employee("Maria", "Santos",
                LocalDate.of(1995, 3, 20), hr, 45000.0);
        hrEmployee.setId(2);

        when(repo.findAll()).thenReturn(List.of(sample, hrEmployee));

        Map<String, List<Employee>> result = service.groupedByDepartment();

        assertEquals(2, result.size());
        assertTrue(result.containsKey("Engineering"));
        assertTrue(result.containsKey("HR"));
        assertEquals(1, result.get("Engineering").size());
        assertEquals(1, result.get("HR").size());
    }

    @Test
    @DisplayName("groupedByDepartment assigns null department to 'Unassigned'")
    void testGroupedByDepartment_nullDepartment() {
        Employee noDept = new Employee();
        noDept.setId(3);
        noDept.setLastname("Ghost");
        noDept.setSalary(30000.0);
        noDept.setDepartment(null);

        when(repo.findAll()).thenReturn(List.of(noDept));

        Map<String, List<Employee>> result = service.groupedByDepartment();

        assertTrue(result.containsKey("Unassigned"));
        assertEquals(1, result.get("Unassigned").size());
    }

    @Test
    @DisplayName("groupedByDepartment returns empty map when no employees")
    void testGroupedByDepartment_empty() {
        when(repo.findAll()).thenReturn(Collections.emptyList());

        Map<String, List<Employee>> result = service.groupedByDepartment();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
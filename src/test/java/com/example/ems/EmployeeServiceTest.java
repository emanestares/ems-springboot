package com.example.ems;

import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Employee;
import com.example.ems.repository.EmployeeRepository;
import com.example.ems.service.EmployeeServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    EmployeeRepository repo;

    @InjectMocks
    EmployeeServiceImpl service;

    private Employee sample;

    @BeforeEach
    void setUp() {
        sample = new Employee("Juan", "Dela Cruz",
                LocalDate.of(1990, 6, 15), "Engineering", 55000.0);
        sample.setId(1);
    }

    // ── FIND ALL ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns all employees")
    void testFindAll_returnsAllEmployees() {
        Employee e2 = new Employee("Maria", "Santos",
                LocalDate.of(1995, 3, 20), "HR", 45000.0);
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
    @DisplayName("findById returns correct employee when found")
    void testFindById_success() {
        when(repo.findById(1)).thenReturn(Optional.of(sample));

        Employee result = service.findById(1);

        assertEquals("Juan", result.getFirstname());
        assertEquals("Dela Cruz", result.getLastname());
        assertEquals("Engineering", result.getDepartment());
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

    // ── SAVE ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save persists a valid employee and returns saved entity")
    void testSave_valid() {
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
    @DisplayName("save throws IllegalArgumentException when salary is negative")
    void testSave_negativeSalary() {
        sample.setSalary(-1.0);

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
        when(repo.save(sample)).thenReturn(sample);

        assertDoesNotThrow(() -> service.save(sample));
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when employee is too young (under 16)")
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
    @DisplayName("save accepts employee with no birthday (null)")
    void testSave_nullBirthday() {
        sample.setBirthday(null);
        when(repo.save(sample)).thenReturn(sample);

        assertDoesNotThrow(() -> service.save(sample));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update modifies all fields and saves")
    void testUpdate_success() {
        Employee updated = new Employee("Pedro", "Reyes",
                LocalDate.of(1988, 1, 10), "Finance", 70000.0);

        when(repo.findById(1)).thenReturn(Optional.of(sample));
        when(repo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.update(1, updated);

        assertEquals("Pedro", result.getFirstname());
        assertEquals("Reyes", result.getLastname());
        assertEquals("Finance", result.getDepartment());
        assertEquals(70000.0, result.getSalary());
        verify(repo, times(1)).save(sample);
    }

    @Test
    @DisplayName("update throws EmployeeNotFoundException when ID does not exist")
    void testUpdate_notFound() {
        Employee updated = new Employee("X", "Y",
                LocalDate.of(1990, 1, 1), "IT", 40000.0);

        when(repo.findById(99)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> service.update(99, updated));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("update throws IllegalArgumentException when updated data is invalid")
    void testUpdate_invalidData() {
        Employee badUpdate = new Employee("", "",
                LocalDate.of(1990, 1, 1), "IT", -500.0);

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

    // ── SEARCH BY NAME ────────────────────────────────────────────────────

    @Test
    @DisplayName("searchByName returns matching employees for a keyword")
    void testSearchByName_withKeyword() {
        when(repo.searchByName("Juan")).thenReturn(List.of(sample));

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
        verify(repo, never()).searchByName(any());
    }

    @Test
    @DisplayName("searchByName returns all employees when keyword is blank")
    void testSearchByName_blankKeyword() {
        when(repo.findAll()).thenReturn(List.of(sample));

        List<Employee> result = service.searchByName("   ");

        assertEquals(1, result.size());
        verify(repo, times(1)).findAll();
        verify(repo, never()).searchByName(any());
    }

    @Test
    @DisplayName("searchByName trims whitespace before searching")
    void testSearchByName_trimsWhitespace() {
        when(repo.searchByName("Juan")).thenReturn(List.of(sample));

        List<Employee> result = service.searchByName("  Juan  ");

        assertEquals(1, result.size());
        verify(repo).searchByName("Juan");
    }

    // ── FIND BY DEPARTMENT ────────────────────────────────────────────────

    @Test
    @DisplayName("findByDepartment returns employees in that department")
    void testFindByDepartment() {
        when(repo.findByDepartmentIgnoreCase("Engineering")).thenReturn(List.of(sample));

        List<Employee> result = service.findByDepartment("Engineering");

        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).getDepartment());
    }

    @Test
    @DisplayName("findByDepartment is case-insensitive")
    void testFindByDepartment_caseInsensitive() {
        when(repo.findByDepartmentIgnoreCase("engineering")).thenReturn(List.of(sample));

        List<Employee> result = service.findByDepartment("engineering");

        assertFalse(result.isEmpty());
        verify(repo).findByDepartmentIgnoreCase("engineering");
    }

    // ── REPORTS ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("reportByDepartment returns employees ordered by department")
    void testReportByDepartment() {
        Employee e2 = new Employee("Ana", "Lopez",
                LocalDate.of(1993, 5, 10), "HR", 40000.0);

        when(repo.findAllByOrderByDepartmentAsc()).thenReturn(List.of(e2, sample));

        List<Employee> result = service.reportByDepartment();

        assertEquals(2, result.size());
        assertEquals("HR", result.get(0).getDepartment());
        verify(repo).findAllByOrderByDepartmentAsc();
    }

    @Test
    @DisplayName("reportByAge returns employees ordered by birthday")
    void testReportByAge() {
        Employee older = new Employee("Carlos", "Gomez",
                LocalDate.of(1975, 1, 1), "IT", 60000.0);

        when(repo.findAllByOrderByBirthdayAsc()).thenReturn(List.of(older, sample));

        List<Employee> result = service.reportByAge();

        assertEquals(2, result.size());
        assertEquals(1975, result.get(0).getBirthday().getYear());
        verify(repo).findAllByOrderByBirthdayAsc();
    }

    // ── AVERAGES ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("averageSalary returns 0 when repository returns null")
    void testAverageSalary_null() {
        when(repo.findAverageSalary()).thenReturn(null);

        assertEquals(0.0, service.averageSalary());
    }

    @Test
    @DisplayName("averageSalary returns correct value from repository")
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
    @DisplayName("averageAge returns correct value from repository")
    void testAverageAge_withData() {
        when(repo.findAverageAge()).thenReturn(32.5);

        assertEquals(32.5, service.averageAge());
    }

    // ── GROUPED BY DEPARTMENT ─────────────────────────────────────────────

    @Test
    @DisplayName("groupedByDepartment groups employees correctly")
    void testGroupedByDepartment() {
        Employee hrEmployee = new Employee("Maria", "Santos",
                LocalDate.of(1995, 3, 20), "HR", 45000.0);
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
        Employee noDept = new Employee("Ghost", "User",
                LocalDate.of(2000, 1, 1), null, 30000.0);
        noDept.setId(3);

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
package com.example.ems;

import com.example.ems.model.Department;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.repository.EmployeeRepository;
import com.example.ems.service.DepartmentServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock DepartmentRepository deptRepo;
    @Mock EmployeeRepository   empRepo;

    @InjectMocks DepartmentServiceImpl service;

    @BeforeEach
    void injectMessages() {
        ReflectionTestUtils.setField(service, "msgNotFound",      "Department not found with ID: ");
        ReflectionTestUtils.setField(service, "msgNameRequired",  "Department name cannot be empty.");
        ReflectionTestUtils.setField(service, "msgNameDuplicate", "A department with that name already exists.");
        ReflectionTestUtils.setField(service, "msgInUse",         "Cannot delete department; employees are currently assigned to it.");
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    void findAll_returnsList() {
        List<Department> list = List.of(new Department("IT"), new Department("HR"));
        when(deptRepo.findAll()).thenReturn(list);

        List<Department> result = service.findAll();

        assertThat(result).hasSize(2);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    void findById_found() {
        Department d = new Department("Finance");
        d.setId(1);
        when(deptRepo.findById(1)).thenReturn(Optional.of(d));

        Department result = service.findById(1);

        assertThat(result.getName()).isEqualTo("Finance");
    }

    @Test
    void findById_notFound_throws() {
        when(deptRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── save ──────────────────────────────────────────────────────────────

    @Test
    void save_valid_savesAndReturns() {
        Department d = new Department("Marketing");
        when(deptRepo.findByNameIgnoreCase("Marketing")).thenReturn(Optional.empty());
        when(deptRepo.save(d)).thenReturn(d);

        Department result = service.save(d);

        assertThat(result.getName()).isEqualTo("Marketing");
        verify(deptRepo).save(d);
    }

    @Test
    void save_blankName_throws() {
        Department d = new Department("");

        assertThatThrownBy(() -> service.save(d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void save_duplicateName_throws() {
        Department existing = new Department("HR");
        existing.setId(5);
        Department d = new Department("hr");

        when(deptRepo.findByNameIgnoreCase("hr")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.save(d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void update_valid_updatesName() {
        Department existing = new Department("Sales");
        existing.setId(2);
        Department updated = new Department("Sales & Marketing");

        when(deptRepo.findById(2)).thenReturn(Optional.of(existing));
        when(deptRepo.findByNameIgnoreCase("Sales & Marketing")).thenReturn(Optional.empty());
        when(deptRepo.save(existing)).thenReturn(existing);

        Department result = service.update(2, updated);

        assertThat(result.getName()).isEqualTo("Sales & Marketing");
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    void delete_noEmployees_deletes() {
        Department d = new Department("Legal");
        d.setId(3);
        when(deptRepo.findById(3)).thenReturn(Optional.of(d));
        when(empRepo.findAll()).thenReturn(Collections.emptyList());

        service.delete(3);

        verify(deptRepo).delete(d);
    }

    @Test
    void delete_withEmployees_throws() {
        Department d = new Department("Engineering");
        d.setId(4);

        com.example.ems.model.Employee emp = new com.example.ems.model.Employee();
        emp.setDepartment(d);

        when(deptRepo.findById(4)).thenReturn(Optional.of(d));
        when(empRepo.findAll()).thenReturn(List.of(emp));

        assertThatThrownBy(() -> service.delete(4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assigned");
    }
}

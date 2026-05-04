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
    @Mock EmployeeRepository   empRepo;   // kept for @InjectMocks wiring; not directly used

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
    @DisplayName("findAll returns all departments")
    void findAll_returnsList() {
        List<Department> list = List.of(new Department("IT"), new Department("HR"));
        when(deptRepo.findAll()).thenReturn(list);

        List<Department> result = service.findAll();

        assertThat(result).hasSize(2);
        verify(deptRepo).findAll();
    }

    @Test
    @DisplayName("findAll returns empty list when none exist")
    void findAll_empty() {
        when(deptRepo.findAll()).thenReturn(Collections.emptyList());

        assertThat(service.findAll()).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns the correct department")
    void findById_found() {
        Department d = new Department("Finance");
        d.setId(1);
        when(deptRepo.findById(1)).thenReturn(Optional.of(d));

        Department result = service.findById(1);

        assertThat(result.getName()).isEqualTo("Finance");
    }

    @Test
    @DisplayName("findById throws RuntimeException when ID not found")
    void findById_notFound_throws() {
        when(deptRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── save ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save persists a new department with a unique name")
    void save_valid_savesAndReturns() {
        Department d = new Department("Marketing");
        when(deptRepo.findByNameIgnoreCase("Marketing")).thenReturn(Optional.empty());
        when(deptRepo.save(d)).thenReturn(d);

        Department result = service.save(d);

        assertThat(result.getName()).isEqualTo("Marketing");
        verify(deptRepo).save(d);
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when name is blank")
    void save_blankName_throws() {
        Department d = new Department("  ");

        assertThatThrownBy(() -> service.save(d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when name is null")
    void save_nullName_throws() {
        Department d = new Department(null);

        assertThatThrownBy(() -> service.save(d))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("save throws IllegalArgumentException when name already exists (case-insensitive)")
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
    @DisplayName("update changes the department name")
    void update_valid_updatesName() {
        Department existing = new Department("Sales");
        existing.setId(2);
        Department updated = new Department("Sales & Marketing");

        when(deptRepo.findById(2)).thenReturn(Optional.of(existing));
        when(deptRepo.findByNameIgnoreCase("Sales & Marketing")).thenReturn(Optional.empty());
        when(deptRepo.save(existing)).thenReturn(existing);

        Department result = service.update(2, updated);

        assertThat(result.getName()).isEqualTo("Sales & Marketing");
        verify(deptRepo).save(existing);
    }

    @Test
    @DisplayName("update allows keeping the same name (no false duplicate)")
    void update_sameName_doesNotThrow() {
        Department existing = new Department("HR");
        existing.setId(3);
        Department updated = new Department("HR");

        when(deptRepo.findById(3)).thenReturn(Optional.of(existing));
        // same name → findByNameIgnoreCase returns the same entity (same id → allowed)
        when(deptRepo.findByNameIgnoreCase("HR")).thenReturn(Optional.of(existing));
        when(deptRepo.save(existing)).thenReturn(existing);

        assertThatCode(() -> service.update(3, updated)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("update throws RuntimeException when department ID does not exist")
    void update_notFound_throws() {
        when(deptRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99, new Department("X")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete removes the department when no employees are assigned")
    void delete_noEmployees_deletes() {
        Department d = new Department("Legal");
        d.setId(3);
        when(deptRepo.findById(3)).thenReturn(Optional.of(d));
        when(deptRepo.countEmployeesByDepartmentId(3)).thenReturn(0L);

        service.delete(3);

        verify(deptRepo).delete(d);
    }

    @Test
    @DisplayName("delete throws IllegalStateException when employees are assigned")
    void delete_withEmployees_throws() {
        Department d = new Department("Engineering");
        d.setId(4);
        when(deptRepo.findById(4)).thenReturn(Optional.of(d));
        when(deptRepo.countEmployeesByDepartmentId(4)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("assigned");

        verify(deptRepo, never()).delete(any());
    }

    @Test
    @DisplayName("delete throws RuntimeException when department ID not found")
    void delete_notFound_throws() {
        when(deptRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(deptRepo, never()).delete(any());
    }
}

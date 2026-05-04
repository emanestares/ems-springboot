package com.example.ems;

import com.example.ems.controller.EmployeeController;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Department;
import com.example.ems.model.Employee;
import com.example.ems.repository.DepartmentRepository;
import com.example.ems.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean EmployeeService      employeeService;
    @MockBean DepartmentRepository deptRepo;          // required by EmployeeController constructor

    private ObjectMapper mapper;
    private Department   dept;
    private Employee     sample;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        dept = new Department("Engineering");
        dept.setId(1);

        sample = new Employee("Juan", "Dela Cruz",
                LocalDate.of(1990, 6, 15), dept, 55000.0);
        sample.setId(1);
    }

    // ── Helper: build a paged response map the way the controller does ────

    private Map<String, Object> pagedResponse(List<Employee> items,
                                               int page, int totalPages, long total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content",       items);
        m.put("currentPage",   page);
        m.put("totalPages",    totalPages);
        m.put("totalElements", total);
        m.put("pageSize",      5);
        return m;
    }

    // ── Helper: minimal EmployeeRequest JSON ─────────────────────────────

    private String empRequestJson(String firstname, String lastname,
                                   String birthday, Integer deptId, double salary) throws Exception {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("firstname",    firstname);
        req.put("lastname",     lastname);
        req.put("birthday",     birthday);
        req.put("departmentId", deptId);
        req.put("salary",       salary);
        return mapper.writeValueAsString(req);
    }

    // ── GET /api/employees ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/employees returns 200 with paginated content")
    void testGetAll_paginated() throws Exception {
        Page<Employee> page = new PageImpl<>(List.of(sample),
                PageRequest.of(0, 5), 1);
        when(employeeService.findAllPaged(0, 5)).thenReturn(page);

        mockMvc.perform(get("/api/employees").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstname", is("Juan")))
                .andExpect(jsonPath("$.content[0].lastname",  is("Dela Cruz")))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages",    is(1)));
    }

    @Test
    @DisplayName("GET /api/employees?search=Juan delegates to searchByNamePaged")
    void testGetAll_withSearch() throws Exception {
        Page<Employee> page = new PageImpl<>(List.of(sample));
        when(employeeService.searchByNamePaged("Juan", 0, 5)).thenReturn(page);

        mockMvc.perform(get("/api/employees")
                        .param("search", "Juan")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstname", is("Juan")));

        verify(employeeService).searchByNamePaged("Juan", 0, 5);
    }

    @Test
    @DisplayName("GET /api/employees?department=Engineering delegates to findByDepartmentPaged")
    void testGetAll_withDepartment() throws Exception {
        Page<Employee> page = new PageImpl<>(List.of(sample));
        when(employeeService.findByDepartmentPaged("Engineering", 0, 5)).thenReturn(page);

        mockMvc.perform(get("/api/employees")
                        .param("department", "Engineering")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].department.name", is("Engineering")));

        verify(employeeService).findByDepartmentPaged("Engineering", 0, 5);
    }

    @Test
    @DisplayName("GET /api/employees returns empty content when no employees exist")
    void testGetAll_empty() throws Exception {
        Page<Employee> empty = new PageImpl<>(Collections.emptyList());
        when(employeeService.findAllPaged(0, 5)).thenReturn(empty);

        mockMvc.perform(get("/api/employees").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ── GET /api/employees/{id} ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/employees/{id} returns 200 with employee")
    void testGetById_found() throws Exception {
        when(employeeService.findById(1)).thenReturn(sample);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",       is(1)))
                .andExpect(jsonPath("$.lastname",  is("Dela Cruz")))
                .andExpect(jsonPath("$.department.name", is("Engineering")));
    }

    @Test
    @DisplayName("GET /api/employees/{id} returns 404 when not found")
    void testGetById_notFound() throws Exception {
        when(employeeService.findById(99))
                .thenThrow(new EmployeeNotFoundException(99));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("99")));
    }

    // ── POST /api/employees ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/employees returns 201 with created employee")
    void testCreate_success() throws Exception {
        when(deptRepo.findById(1)).thenReturn(Optional.of(dept));
        when(employeeService.save(any(Employee.class))).thenReturn(sample);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(empRequestJson("Juan", "Dela Cruz",
                                "1990-06-15", 1, 55000.0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",        is(1)))
                .andExpect(jsonPath("$.firstname",  is("Juan")))
                .andExpect(jsonPath("$.department.name", is("Engineering")));
    }

    @Test
    @DisplayName("POST /api/employees returns 400 when service throws IllegalArgumentException")
    void testCreate_illegalArgument() throws Exception {
        when(deptRepo.findById(1)).thenReturn(Optional.of(dept));
        when(employeeService.save(any(Employee.class)))
                .thenThrow(new IllegalArgumentException("Salary cannot be negative"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(empRequestJson("Juan", "Dela Cruz",
                                "1990-06-15", 1, -100.0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Salary")));
    }

    // ── PUT /api/employees/{id} ───────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/employees/{id} returns updated employee")
    void testUpdate_success() throws Exception {
        Department finance = new Department("Finance");
        finance.setId(2);
        Employee updated = new Employee("Pedro", "Reyes",
                LocalDate.of(1988, 1, 10), finance, 70000.0);
        updated.setId(1);

        when(employeeService.findById(1)).thenReturn(sample);
        when(deptRepo.findById(2)).thenReturn(Optional.of(finance));
        when(employeeService.update(eq(1), any(Employee.class))).thenReturn(updated);

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(empRequestJson("Pedro", "Reyes",
                                "1988-01-10", 2, 70000.0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname",       is("Pedro")))
                .andExpect(jsonPath("$.department.name", is("Finance")));
    }

    @Test
    @DisplayName("PUT /api/employees/{id} returns 404 when employee not found")
    void testUpdate_notFound() throws Exception {
        when(employeeService.findById(99))
                .thenThrow(new EmployeeNotFoundException(99));

        mockMvc.perform(put("/api/employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(empRequestJson("X", "Y",
                                "1990-01-01", 1, 40000.0)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/employees/{id} ────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/employees/{id} returns 204 No Content")
    void testDelete_success() throws Exception {
        doNothing().when(employeeService).delete(1);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService, times(1)).delete(1);
    }

    @Test
    @DisplayName("DELETE /api/employees/{id} returns 404 when not found")
    void testDelete_notFound() throws Exception {
        doThrow(new EmployeeNotFoundException(99))
                .when(employeeService).delete(99);

        mockMvc.perform(delete("/api/employees/99"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/employees/{id}/toggle-active ───────────────────────────

    @Test
    @DisplayName("PATCH /api/employees/{id}/toggle-active flips isActive")
    void testToggleActive_deactivates() throws Exception {
        Employee deactivated = new Employee("Juan", "Dela Cruz",
                LocalDate.of(1990, 6, 15), dept, 55000.0);
        deactivated.setId(1);
        deactivated.setIsActive(false);

        when(employeeService.toggleActive(1)).thenReturn(deactivated);

        mockMvc.perform(patch("/api/employees/1/toggle-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", is(false)));
    }

    // ── GET /api/employees/stats ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/employees/stats returns aggregated statistics (default: active)")
    void testGetStats_active() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of(sample));
        when(employeeService.countActive()).thenReturn(1L);
        when(employeeService.averageSalaryActive()).thenReturn(55000.0);
        when(employeeService.averageAgeActive()).thenReturn(34.5);
        when(employeeService.groupedByDepartmentActive())
                .thenReturn(Map.of("Engineering", List.of(sample)));

        mockMvc.perform(get("/api/employees/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees",  is(1)))
                .andExpect(jsonPath("$.activeEmployees", is(1)))
                .andExpect(jsonPath("$.averageSalary",   is(55000.0)))
                .andExpect(jsonPath("$.averageAge",      is(34.5)))
                .andExpect(jsonPath("$.byDepartment.Engineering", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/employees/stats?filter=all uses overall aggregates")
    void testGetStats_all() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of(sample));
        when(employeeService.countActive()).thenReturn(1L);
        when(employeeService.averageSalary()).thenReturn(55000.0);
        when(employeeService.averageAge()).thenReturn(34.5);
        when(employeeService.groupedByDepartment())
                .thenReturn(Map.of("Engineering", List.of(sample)));

        mockMvc.perform(get("/api/employees/stats").param("filter", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter", is("all")))
                .andExpect(jsonPath("$.totalEmployees", is(1)));
    }

    @Test
    @DisplayName("GET /api/employees/stats?filter=inactive uses inactive aggregates")
    void testGetStats_inactive() throws Exception {
        Employee inactive = new Employee("Jane", "Doe",
                LocalDate.of(1992, 4, 1), dept, 40000.0);
        inactive.setId(2);
        inactive.setIsActive(false);

        when(employeeService.findAll()).thenReturn(List.of(sample, inactive));
        when(employeeService.countActive()).thenReturn(1L);
        when(employeeService.averageSalaryInactive()).thenReturn(40000.0);
        when(employeeService.averageAgeInactive()).thenReturn(33.0);
        when(employeeService.groupedByDepartmentInactive())
                .thenReturn(Map.of("Engineering", List.of(inactive)));

        mockMvc.perform(get("/api/employees/stats").param("filter", "inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter",        is("inactive")))
                .andExpect(jsonPath("$.totalEmployees", is(2)));
    }

    // ── GET /api/employees/report/* ───────────────────────────────────────

    @Test
    @DisplayName("GET /api/employees/report/by-department returns ordered list")
    void testReportByDepartment() throws Exception {
        when(employeeService.reportByDepartment()).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/employees/report/by-department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/employees/report/by-age returns ordered list")
    void testReportByAge() throws Exception {
        when(employeeService.reportByAge()).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/employees/report/by-age"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}

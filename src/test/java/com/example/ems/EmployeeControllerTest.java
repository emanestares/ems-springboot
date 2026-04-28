package com.example.ems;

import com.example.ems.controller.EmployeeController;
import com.example.ems.exception.EmployeeNotFoundException;
import com.example.ems.model.Employee;
import com.example.ems.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @Autowired
    MockMvc mockMvc;

    @MockBean
    EmployeeService employeeService;

    private ObjectMapper mapper;
    private Employee sample;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        sample = new Employee("Juan", "Dela Cruz",
                LocalDate.of(1990, 6, 15), "Engineering", 55000.0);
        sample.setId(1);
    }

    // ── GET /api/employees ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/employees returns 200 with all employees")
    void testGetAll() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstname", is("Juan")))
                .andExpect(jsonPath("$[0].lastname", is("Dela Cruz")));
    }

    @Test
    @DisplayName("GET /api/employees?search=Juan returns search results")
    void testGetAll_withSearch() throws Exception {
        when(employeeService.searchByName("Juan")).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/employees").param("search", "Juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstname", is("Juan")));

        verify(employeeService).searchByName("Juan");
    }

    @Test
    @DisplayName("GET /api/employees?department=Engineering returns filtered results")
    void testGetAll_withDepartment() throws Exception {
        when(employeeService.findByDepartment("Engineering")).thenReturn(List.of(sample));

        mockMvc.perform(get("/api/employees").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department", is("Engineering")));

        verify(employeeService).findByDepartment("Engineering");
    }

    // ── GET /api/employees/{id} ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/employees/{id} returns 200 with employee")
    void testGetById_found() throws Exception {
        when(employeeService.findById(1)).thenReturn(sample);

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.lastname", is("Dela Cruz")));
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
        when(employeeService.save(any(Employee.class))).thenReturn(sample);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sample)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.firstname", is("Juan")));
    }

    @Test
    @DisplayName("POST /api/employees returns 400 when validation fails (blank lastname)")
    void testCreate_blankLastname() throws Exception {
        Employee invalid = new Employee("Juan", "",
                LocalDate.of(1990, 6, 15), "Engineering", 55000.0);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/employees returns 400 when service throws IllegalArgumentException")
    void testCreate_illegalArgument() throws Exception {
        when(employeeService.save(any(Employee.class)))
                .thenThrow(new IllegalArgumentException("Salary cannot be negative"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sample)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Salary")));
    }

    // ── PUT /api/employees/{id} ───────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/employees/{id} returns updated employee")
    void testUpdate_success() throws Exception {
        Employee updated = new Employee("Pedro", "Reyes",
                LocalDate.of(1988, 1, 10), "Finance", 70000.0);
        updated.setId(1);

        when(employeeService.update(eq(1), any(Employee.class))).thenReturn(updated);

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname", is("Pedro")))
                .andExpect(jsonPath("$.department", is("Finance")));
    }

    @Test
    @DisplayName("PUT /api/employees/{id} returns 404 when employee not found")
    void testUpdate_notFound() throws Exception {
        when(employeeService.update(eq(99), any(Employee.class)))
                .thenThrow(new EmployeeNotFoundException(99));

        mockMvc.perform(put("/api/employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(sample)))
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

    // ── GET /api/employees/stats ──────────────────────────────────────────

    @Test
    @DisplayName("GET /api/employees/stats returns aggregated statistics")
    void testGetStats() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of(sample));
        when(employeeService.averageSalary()).thenReturn(55000.0);
        when(employeeService.averageAge()).thenReturn(34.5);
        when(employeeService.groupedByDepartment())
                .thenReturn(Map.of("Engineering", List.of(sample)));

        mockMvc.perform(get("/api/employees/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees", is(1)))
                .andExpect(jsonPath("$.averageSalary", is(55000.0)))
                .andExpect(jsonPath("$.averageAge", is(34.5)))
                .andExpect(jsonPath("$.byDepartment.Engineering", hasSize(1)));
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
package com.example.ems.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee extends Person {   // INHERITANCE

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Last name is required")
    @Column(name = "lastname", nullable = false)
    private String lastname;  // shadowed from Person for validation

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "birthday")
    private LocalDate birthday;

    @NotBlank(message = "Department is required")
    @Column(name = "department")
    private String department;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", message = "Salary must be non-negative")
    @Column(name = "salary")
    private Double salary;

    @Column(name = "isActive", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true;

    // --- Constructors ---
    public Employee() { super(); }

    public Employee(String firstname, String lastname,
                    LocalDate birthday, String department, Double salary) {
        super(firstname, lastname, birthday);
        this.firstname  = firstname;
        this.lastname   = lastname;
        this.birthday   = birthday;
        this.department = department;
        this.salary     = salary;
        this.isActive   = true;
    }

    // POLYMORPHISM: Override the abstract method from Person
    @Override
    public String getDisplayName() {
        return firstname + " " + lastname;
    }

    // --- Getters and Setters ---
    public Integer getId()          { return id; }
    public String getDepartment()   { return department; }
    public void setDepartment(String d) { this.department = d; }
    public Double getSalary()       { return salary; }
    public void setSalary(Double s) { this.salary = s; }
    public void setId(Integer id) { this.id = id; }
    public Boolean getIsActive()    { return isActive != null ? isActive : true; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // Override Person's getters to ensure JPA reads the right field
    @Override public String getFirstname() { return firstname; }
    @Override public void setFirstname(String v) { this.firstname = v; super.setFirstname(v); }
    @Override public String getLastname()  { return lastname; }
    @Override public void setLastname(String v)  { this.lastname = v; super.setLastname(v); }
    @Override public LocalDate getBirthday() { return birthday; }
    @Override public void setBirthday(LocalDate v) { this.birthday = v; super.setBirthday(v); }
}

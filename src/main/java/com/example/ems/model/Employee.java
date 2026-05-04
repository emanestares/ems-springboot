package com.example.ems.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee extends Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Last name is required")
    @Column(name = "lastname", nullable = false)
    private String lastname;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "birthday")
    private LocalDate birthday;

    @NotNull(message = "Department is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", message = "Salary must be non-negative")
    @Column(name = "salary")
    private Double salary;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true;

    public Employee() { super(); }

    public Employee(String firstname, String lastname,
                    LocalDate birthday, Department department, Double salary) {
        super(firstname, lastname, birthday);
        this.firstname  = firstname;
        this.lastname   = lastname;
        this.birthday   = birthday;
        this.department = department;
        this.salary     = salary;
        this.isActive   = true;
    }

    @Override
    public String getDisplayName() {
        return firstname + " " + lastname;
    }

    // Convenience method — returns dept name for reports/PDF (never null-safe issue)
    public String getDepartmentName() {
        return department != null ? department.getName() : "—";
    }

    public Integer getId()              { return id; }
    public void setId(Integer id)       { this.id = id; }
    public Department getDepartment()   { return department; }
    public void setDepartment(Department d) { this.department = d; }
    public Double getSalary()           { return salary; }
    public void setSalary(Double s)     { this.salary = s; }
    public Boolean getIsActive()        { return isActive != null ? isActive : true; }
    public void setIsActive(Boolean a)  { this.isActive = a; }

    @Override public String getFirstname()            { return firstname; }
    @Override public void setFirstname(String v)      { this.firstname = v; super.setFirstname(v); }
    @Override public String getLastname()             { return lastname; }
    @Override public void setLastname(String v)       { this.lastname = v; super.setLastname(v); }
    @Override public LocalDate getBirthday()          { return birthday; }
    @Override public void setBirthday(LocalDate v)    { this.birthday = v; super.setBirthday(v); }
}

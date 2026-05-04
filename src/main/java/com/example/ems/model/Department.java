package com.example.ems.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Department name is required")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public Department() {}

    public Department(String name) {
        this.name = name;
    }

    public Integer getId()       { return id; }
    public void setId(Integer id){ this.id = id; }
    public String getName()      { return name; }
    public void setName(String n){ this.name = n; }
}

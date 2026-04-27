package com.example.ems.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Admin entity – matches the admin table schema:
 *   id (PK, auto_increment), lastname, firstname, email (UNIQUE NOT NULL), password (NOT NULL)
 *
 * NOTE: The admin table does NOT have a birthday column, so we do NOT extend Person here.
 *       This avoids any JPA mapping conflicts with the inherited birthday field.
 */
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "firstname")
    private String firstname;

    @NotBlank(message = "Last name is required")
    @Column(name = "lastname")
    private String lastname;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)
    private String password;

    // ── Constructors ───────────────────────────────────────────────────────
    public Admin() {}

    public Admin(String firstname, String lastname, String email, String password) {
        this.firstname = firstname;
        this.lastname  = lastname;
        this.email     = email;
        this.password  = password;
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    public String getDisplayName() {
        return (firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "");
    }

    // ── Getters & Setters ──────────────────────────────────────────────────
    public Integer getId()               { return id; }
    public void    setId(Integer id)     { this.id = id; }

    public String  getFirstname()        { return firstname; }
    public void    setFirstname(String v){ this.firstname = v; }

    public String  getLastname()         { return lastname; }
    public void    setLastname(String v) { this.lastname = v; }

    public String  getEmail()            { return email; }
    public void    setEmail(String v)    { this.email = v; }

    public String  getPassword()         { return password; }
    public void    setPassword(String v) { this.password = v; }
}
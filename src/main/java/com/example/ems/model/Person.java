package com.example.ems.model;

import java.time.LocalDate;
import java.time.Period;

public abstract class Person {

    private String firstname;
    private String lastname;
    private LocalDate birthday;

    // Constructor
    protected Person(String firstname, String lastname, LocalDate birthday) {
        this.firstname = firstname;
        this.lastname  = lastname;
        this.birthday  = birthday;
    }

    protected Person() {}

    // Getters and Setters
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname()  { return lastname; }
    public void setLastname(String lastname)   { this.lastname = lastname; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public abstract String getDisplayName();

    // Concrete helper method available to all subclasses
    public int getAge() {
        if (birthday == null) return 0;
        return Period.between(birthday, LocalDate.now()).getYears();
    }
}
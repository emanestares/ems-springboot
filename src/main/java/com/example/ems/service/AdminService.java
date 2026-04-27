package com.example.ems.service;

import com.example.ems.model.Admin;

import java.util.Optional;

public interface AdminService {

    Admin register(Admin admin);

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean checkPassword(String rawPassword, String encodedPassword);
}
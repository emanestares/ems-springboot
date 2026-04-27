package com.example.ems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Holds all request/response DTOs for the Auth endpoints.
 * Bundled in one file for convenience – you can split them
 * into separate files if you prefer.
 */
public class AuthDtos {

    // ── Login ──────────────────────────────────────────────────────────────

    public static class LoginRequest {

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail()             { return email; }
        public void   setEmail(String email) { this.email = email; }
        public String getPassword()          { return password; }
        public void   setPassword(String pw) { this.password = pw; }
    }

    // ── Register ───────────────────────────────────────────────────────────

    public static class RegisterRequest {

        @NotBlank(message = "First name is required")
        private String firstname;

        @NotBlank(message = "Last name is required")
        private String lastname;

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        public String getFirstname()               { return firstname; }
        public void   setFirstname(String v)       { this.firstname = v; }
        public String getLastname()                { return lastname; }
        public void   setLastname(String v)        { this.lastname = v; }
        public String getEmail()                   { return email; }
        public void   setEmail(String v)           { this.email = v; }
        public String getPassword()                { return password; }
        public void   setPassword(String v)        { this.password = v; }
    }

    // ── Shared success response ────────────────────────────────────────────

    public static class AuthResponse {

        private String message;
        private String firstname;
        private String lastname;
        private String email;
        private Integer adminId;

        public AuthResponse() {}

        public AuthResponse(String message, String firstname,
                            String lastname, String email, Integer adminId) {
            this.message   = message;
            this.firstname = firstname;
            this.lastname  = lastname;
            this.email     = email;
            this.adminId   = adminId;
        }

        public String  getMessage()            { return message; }
        public void    setMessage(String v)    { this.message = v; }
        public String  getFirstname()          { return firstname; }
        public void    setFirstname(String v)  { this.firstname = v; }
        public String  getLastname()           { return lastname; }
        public void    setLastname(String v)   { this.lastname = v; }
        public String  getEmail()              { return email; }
        public void    setEmail(String v)      { this.email = v; }
        public Integer getAdminId()            { return adminId; }
        public void    setAdminId(Integer v)   { this.adminId = v; }
    }

    // ── Error response ─────────────────────────────────────────────────────

    public static class ErrorResponse {

        private String error;

        public ErrorResponse(String error) { this.error = error; }

        public String getError()           { return error; }
        public void   setError(String v)   { this.error = v; }
    }
}
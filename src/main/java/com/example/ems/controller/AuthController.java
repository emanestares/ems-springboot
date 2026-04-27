package com.example.ems.controller;

import com.example.ems.dto.AuthDtos;
import com.example.ems.dto.AuthDtos.*;
import com.example.ems.model.Admin;
import com.example.ems.service.AdminService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class AuthController {

    private static final String SESSION_ADMIN_ID    = "adminId";
    private static final String SESSION_ADMIN_EMAIL = "adminEmail";
    private static final String SESSION_ADMIN_NAME  = "adminName";

    private final AdminService adminService;

    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ── POST /api/auth/register ────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            Admin admin = new Admin();
            admin.setFirstname(req.getFirstname());
            admin.setLastname(req.getLastname());
            admin.setEmail(req.getEmail());
            admin.setPassword(req.getPassword());

            Admin saved = adminService.register(admin);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new AuthResponse("Registration successful",
                            saved.getFirstname(), saved.getLastname(),
                            saved.getEmail(), saved.getId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(ex.getMessage()));
        }
    }

    // ── POST /api/auth/login ───────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpSession session) {
        Optional<Admin> adminOpt = adminService.findByEmail(req.getEmail());

        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password."));
        }

        Admin admin = adminOpt.get();

        if (!adminService.checkPassword(req.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid email or password."));
        }

        // Store minimal info in session
        session.setAttribute(SESSION_ADMIN_ID,    admin.getId());
        session.setAttribute(SESSION_ADMIN_EMAIL, admin.getEmail());
        session.setAttribute(SESSION_ADMIN_NAME,  admin.getDisplayName());

        return ResponseEntity.ok(
                new AuthResponse("Login successful",
                        admin.getFirstname(), admin.getLastname(),
                        admin.getEmail(), admin.getId()));
    }

    // ── POST /api/auth/logout ──────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // ── GET /api/auth/me ───────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Integer adminId = (Integer) session.getAttribute(SESSION_ADMIN_ID);
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Not logged in."));
        }
        return adminService.findByEmail(
                        (String) session.getAttribute(SESSION_ADMIN_EMAIL))
                .map(a -> ResponseEntity.ok(
                        new AuthResponse("Authenticated",
                                a.getFirstname(), a.getLastname(),
                                a.getEmail(), a.getId())))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthResponse("Session expired", null, null, null, null)));
    }

    // ── Global validation error handler for this controller ───────────────
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(new ErrorResponse(msg));
    }
}
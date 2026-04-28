package com.example.ems;

import com.example.ems.model.Admin;
import com.example.ems.repository.AdminRepository;
import com.example.ems.service.AdminServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    AdminRepository adminRepository;

    @InjectMocks
    AdminServiceImpl service;

    private Admin sample;

    @BeforeEach
    void setUp() {
        sample = new Admin("Juan", "Dela Cruz", "juan@example.com", "password123");
        sample.setId(1);
    }

    // ── REGISTER ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register saves a new admin with hashed password")
    void testRegister_success() {
        when(adminRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> {
            Admin a = inv.getArgument(0);
            a.setId(1);
            return a;
        });

        Admin result = service.register(sample);

        assertNotNull(result);
        assertEquals("juan@example.com", result.getEmail());
        // Password must be BCrypt-hashed, not plain text
        assertNotEquals("password123", result.getPassword());
        assertTrue(result.getPassword().startsWith("$2a$") || result.getPassword().startsWith("$2b$"));
        verify(adminRepository, times(1)).save(any(Admin.class));
    }

    @Test
    @DisplayName("register throws IllegalArgumentException when email already exists")
    void testRegister_duplicateEmail() {
        when(adminRepository.existsByEmail("juan@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(sample));

        assertTrue(ex.getMessage().contains("already registered"));
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("register hashes the password before persisting")
    void testRegister_passwordIsHashed() {
        String rawPassword = "mySecret99";
        sample.setPassword(rawPassword);

        when(adminRepository.existsByEmail(sample.getEmail())).thenReturn(false);
        when(adminRepository.save(any(Admin.class))).thenAnswer(inv -> inv.getArgument(0));

        Admin result = service.register(sample);

        // Verify it is a valid bcrypt hash
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(rawPassword, result.getPassword()));
    }

    // ── FIND BY EMAIL ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmail returns the admin when found")
    void testFindByEmail_found() {
        when(adminRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(sample));

        Optional<Admin> result = service.findByEmail("juan@example.com");

        assertTrue(result.isPresent());
        assertEquals("juan@example.com", result.get().getEmail());
    }

    @Test
    @DisplayName("findByEmail returns empty optional when not found")
    void testFindByEmail_notFound() {
        when(adminRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Optional<Admin> result = service.findByEmail("unknown@example.com");

        assertFalse(result.isPresent());
    }

    // ── EXISTS BY EMAIL ───────────────────────────────────────────────────

    @Test
    @DisplayName("existsByEmail returns true when email is registered")
    void testExistsByEmail_true() {
        when(adminRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertTrue(service.existsByEmail("juan@example.com"));
    }

    @Test
    @DisplayName("existsByEmail returns false when email is not registered")
    void testExistsByEmail_false() {
        when(adminRepository.existsByEmail("nobody@example.com")).thenReturn(false);

        assertFalse(service.existsByEmail("nobody@example.com"));
    }

    // ── CHECK PASSWORD ────────────────────────────────────────────────────

    @Test
    @DisplayName("checkPassword returns true when raw password matches encoded")
    void testCheckPassword_correct() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("password123");

        assertTrue(service.checkPassword("password123", encoded));
    }

    @Test
    @DisplayName("checkPassword returns false when raw password does not match")
    void testCheckPassword_wrong() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("password123");

        assertFalse(service.checkPassword("wrongPassword", encoded));
    }

    @Test
    @DisplayName("checkPassword returns false for empty password against hash")
    void testCheckPassword_emptyPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode("password123");

        assertFalse(service.checkPassword("", encoded));
    }
}
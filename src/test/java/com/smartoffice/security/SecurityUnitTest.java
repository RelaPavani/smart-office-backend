package com.smartoffice.security;

import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUnitTest {

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        // 256-bit test secret
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 3600000);
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("Requirement 1: Password is BCrypt encoded and never plain text")
    void testPasswordIsBcryptEncoded() {
        String rawPassword = "SecurePassword123!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertFalse(encodedPassword.equals(rawPassword));
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("WrongPassword", encodedPassword));
    }

    @Test
    @DisplayName("JWT Service: Generate, extract claims and validate token")
    void testJwtServiceOperations() {
        String username = "testuser";
        String role = "MANAGER";

        String token = jwtService.generateToken(username, role);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(username, jwtService.extractUsername(token));
        assertEquals(role, jwtService.extractRole(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("JWT Service: Invalid and malformed tokens fail validation")
    void testInvalidJwtValidation() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    @DisplayName("Requirement 7, 8, 9: Authorities are correctly mapped with ROLE_ prefix for all roles")
    void testAuthoritiesRoleMapping() {
        // ADMIN
        User admin = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@smartoffice.com")
                .password(passwordEncoder.encode("adminPass"))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        UserDetails adminDetails = userDetailsService.loadUserByUsername("admin");
        assertTrue(adminDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        // MANAGER (via email lookup)
        User manager = User.builder()
                .id(2L)
                .username("manager1")
                .email("manager@smartoffice.com")
                .password(passwordEncoder.encode("mgrPass"))
                .role(Role.MANAGER)
                .enabled(true)
                .build();
        when(userRepository.findByUsername("manager@smartoffice.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("manager@smartoffice.com")).thenReturn(Optional.of(manager));
        UserDetails managerDetails = userDetailsService.loadUserByUsername("manager@smartoffice.com");
        assertTrue(managerDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));

        // EMPLOYEE
        User employee = User.builder()
                .id(3L)
                .username("emp1")
                .email("emp@smartoffice.com")
                .password(passwordEncoder.encode("empPass"))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build();
        when(userRepository.findByUsername("emp1")).thenReturn(Optional.of(employee));
        UserDetails employeeDetails = userDetailsService.loadUserByUsername("emp1");
        assertTrue(employeeDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")));
    }

    @Test
    @DisplayName("UserDetailsService: Throws UsernameNotFoundException for non-existent user")
    void testUserNotFoundThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown"));
    }
}

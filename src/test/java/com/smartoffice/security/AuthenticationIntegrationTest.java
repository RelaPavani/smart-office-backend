package com.smartoffice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoffice.dto.auth.LoginRequest;
import com.smartoffice.dto.user.UserCreateRequest;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    private static final String TEST_ADMIN_USER = "test_admin";
    private static final String TEST_MGR_USER = "test_manager";
    private static final String TEST_EMP_USER = "test_employee";
    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setupTestData() {
        if (!userRepository.existsByUsername(TEST_ADMIN_USER)) {
            userRepository.save(User.builder()
                    .username(TEST_ADMIN_USER)
                    .email("test_admin@smartoffice.com")
                    .password(passwordEncoder.encode(TEST_PASSWORD))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        }

        if (!userRepository.existsByUsername(TEST_MGR_USER)) {
            userRepository.save(User.builder()
                    .username(TEST_MGR_USER)
                    .email("test_mgr@smartoffice.com")
                    .password(passwordEncoder.encode(TEST_PASSWORD))
                    .role(Role.MANAGER)
                    .enabled(true)
                    .build());
        }

        if (!userRepository.existsByUsername(TEST_EMP_USER)) {
            userRepository.save(User.builder()
                    .username(TEST_EMP_USER)
                    .email("test_emp@smartoffice.com")
                    .password(passwordEncoder.encode(TEST_PASSWORD))
                    .role(Role.EMPLOYEE)
                    .enabled(true)
                    .build());
        }
    }

    @Test
    @DisplayName("Requirement 1: BCrypt password hashing is enforced on stored users")
    void testStoredPasswordIsBcrypt() {
        User user = userRepository.findByUsername(TEST_ADMIN_USER).orElseThrow();
        assertNotNull(user.getPassword());
        assertTrue(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, user.getPassword()));
        assertFalse(user.getPassword().contains(TEST_PASSWORD));
    }

    @Test
    @DisplayName("Requirement 2: Login with valid credentials succeeds and returns JWT token")
    void testLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(TEST_ADMIN_USER)
                .password(TEST_PASSWORD)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.username").value(TEST_ADMIN_USER))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Requirement 3: Login with invalid password fails with 401 Unauthorized")
    void testLoginInvalidPasswordFails() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username(TEST_ADMIN_USER)
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Requirement 4: Protected endpoint without JWT returns 401 Unauthorized")
    void testProtectedEndpointWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("Requirement 5: Protected endpoint with invalid JWT returns 401 Unauthorized")
    void testProtectedEndpointWithInvalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Requirement 6: Valid JWT authenticates the user on protected endpoint")
    void testValidJwtAuthenticatesUser() throws Exception {
        String token = jwtService.generateToken(TEST_ADMIN_USER, "ADMIN");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(TEST_ADMIN_USER))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Requirement 7: ADMIN authority is correctly assigned and verified")
    void testAdminAuthority() throws Exception {
        String adminToken = jwtService.generateToken(TEST_ADMIN_USER, "ADMIN");

        mockMvc.perform(get("/api/auth/admin-only")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Requirement 8: MANAGER authority is correctly assigned and access control enforced")
    void testManagerAuthority() throws Exception {
        String managerToken = jwtService.generateToken(TEST_MGR_USER, "MANAGER");

        // Manager can access manager-only
        mockMvc.perform(get("/api/auth/manager-only")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Manager is forbidden from admin-only
        mockMvc.perform(get("/api/auth/admin-only")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You do not have permission to access this resource"));
    }

    @Test
    @DisplayName("Requirement 9: EMPLOYEE authority is correctly assigned and access control enforced")
    void testEmployeeAuthority() throws Exception {
        String employeeToken = jwtService.generateToken(TEST_EMP_USER, "EMPLOYEE");

        // Employee can access employee-only
        mockMvc.perform(get("/api/auth/employee-only")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Employee is forbidden from admin-only
        mockMvc.perform(get("/api/auth/admin-only")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Requirement 10: Public health endpoint remains accessible without JWT")
    void testPublicHealthEndpointAccessibleWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Service Layer: User creation enforces uniqueness and password encoding")
    void testUserServiceCreateUser() {
        String newUsername = "new_service_user_" + System.currentTimeMillis();
        String newEmail = "new_user_" + System.currentTimeMillis() + "@smartoffice.com";

        UserCreateRequest createRequest = UserCreateRequest.builder()
                .username(newUsername)
                .email(newEmail)
                .password("SecureSecret123!")
                .role(Role.EMPLOYEE)
                .build();

        var created = userService.createUser(createRequest);
        assertNotNull(created.getId());
        org.junit.jupiter.api.Assertions.assertEquals(newUsername, created.getUsername());
        org.junit.jupiter.api.Assertions.assertEquals(Role.EMPLOYEE, created.getRole());

        // Verify password in DB is BCrypt hashed
        User dbUser = userRepository.findById(created.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("SecureSecret123!", dbUser.getPassword()));
    }
}

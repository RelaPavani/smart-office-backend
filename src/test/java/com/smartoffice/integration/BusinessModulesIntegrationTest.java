package com.smartoffice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoffice.dto.department.DepartmentCreateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.repository.DepartmentRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessModulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String employeeToken;
    private Department testDept;
    private Employee testEmployee;

    @BeforeEach
    void setupTestData() {
        // Setup Admin User
        String adminUsername = "integ_admin";
        User adminUser = userRepository.findByUsername(adminUsername).orElseGet(() ->
                userRepository.save(User.builder()
                        .username(adminUsername)
                        .email("integ_admin@smartoffice.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build())
        );
        adminToken = jwtService.generateToken(adminUsername, "ADMIN");

        // Setup Department
        String deptName = "Integ_Engineering_" + System.currentTimeMillis();
        testDept = departmentRepository.findByName("Integ_Engineering").orElseGet(() ->
                departmentRepository.save(Department.builder()
                        .name("Integ_Engineering")
                        .description("Test department")
                        .active(true)
                        .build())
        );

        // Setup Employee User & Employee Record
        String empUsername = "integ_emp";
        User empUser = userRepository.findByUsername(empUsername).orElseGet(() ->
                userRepository.save(User.builder()
                        .username(empUsername)
                        .email("integ_emp@smartoffice.com")
                        .password(passwordEncoder.encode("Password123!"))
                        .role(Role.EMPLOYEE)
                        .enabled(true)
                        .build())
        );
        employeeToken = jwtService.generateToken(empUsername, "EMPLOYEE");

        testEmployee = employeeRepository.findByEmail("integ_emp@smartoffice.com").orElseGet(() ->
                employeeRepository.save(Employee.builder()
                        .employeeCode("INT_EMP_" + System.currentTimeMillis())
                        .firstName("Integration")
                        .lastName("Tester")
                        .email("integ_emp@smartoffice.com")
                        .designation("QA Engineer")
                        .joiningDate(LocalDate.now().minusDays(10))
                        .status(EmployeeStatus.ACTIVE)
                        .department(testDept)
                        .user(empUser)
                        .build())
        );
    }

    @Test
    @DisplayName("Admin creates department successfully via API")
    void testAdminCreatesDepartment() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("Dept_" + System.currentTimeMillis())
                .description("Automated department")
                .active(true)
                .build();

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(request.getName()));
    }

    @Test
    @DisplayName("Employee cannot create department (403 Forbidden)")
    void testEmployeeCannotCreateDepartment() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("ForbiddenDept_" + System.currentTimeMillis())
                .build();

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee can view own profile via /api/employees/me")
    void testEmployeeCanViewOwnProfile() throws Exception {
        mockMvc.perform(get("/api/employees/me")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("integ_emp@smartoffice.com"));
    }

    @Test
    @DisplayName("Employee cannot access another employee's profile by ID")
    void testEmployeeCannotAccessAnotherEmployeeProfile() throws Exception {
        mockMvc.perform(get("/api/employees/999999")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Protected business endpoint without JWT returns 401 Unauthorized")
    void testBusinessEndpointWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/employees/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/attendance/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/leaves/my"))
                .andExpect(status().isUnauthorized());
    }
}

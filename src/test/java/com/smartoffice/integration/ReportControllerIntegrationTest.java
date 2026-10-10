package com.smartoffice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private String managerToken;
    private String employeeToken;

    private Department dept;
    private Employee managerEmp;
    private Employee regularEmp;

    @BeforeEach
    void setUp() {
        dept = departmentRepository.save(Department.builder()
                .name("Rep_Dept_" + System.currentTimeMillis())
                .description("Report Test Department")
                .active(true)
                .build());

        String adminUser = "rep_admin_" + System.currentTimeMillis();
        userRepository.save(User.builder()
                .username(adminUser)
                .email(adminUser + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
        adminToken = jwtService.generateToken(adminUser, "ADMIN");

        String mgrUser = "rep_mgr_" + System.currentTimeMillis();
        User mu = userRepository.save(User.builder()
                .username(mgrUser)
                .email(mgrUser + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.MANAGER)
                .enabled(true)
                .build());
        managerToken = jwtService.generateToken(mgrUser, "MANAGER");

        managerEmp = employeeRepository.save(Employee.builder()
                .employeeCode("REP_MGR_" + System.currentTimeMillis())
                .firstName("Report")
                .lastName("Manager")
                .email(mgrUser + "@smartoffice.com")
                .designation("Engineering Manager")
                .joiningDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .department(dept)
                .user(mu)
                .build());

        String empUser = "rep_emp_" + System.currentTimeMillis();
        User eu = userRepository.save(User.builder()
                .username(empUser)
                .email(empUser + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build());
        employeeToken = jwtService.generateToken(empUser, "EMPLOYEE");

        regularEmp = employeeRepository.save(Employee.builder()
                .employeeCode("REP_EMP_" + System.currentTimeMillis())
                .firstName("Report")
                .lastName("Developer")
                .email(empUser + "@smartoffice.com")
                .designation("Developer")
                .joiningDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .department(dept)
                .manager(managerEmp)
                .user(eu)
                .build());
    }

    @Test
    @DisplayName("Unauthenticated report endpoints return 401")
    void unauthenticatedReportEndpoints_return401() throws Exception {
        mockMvc.perform(get("/api/reports/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/attendance")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/leaves")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/complaints")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/dashboard/admin")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/dashboard/manager")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reports/dashboard/employee")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin dashboard accessible only by ADMIN")
    void adminDashboard_roleCheck() throws Exception {
        // Admin access -> 200
        mockMvc.perform(get("/api/reports/dashboard/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEmployees").isNumber());

        // Employee access -> 403
        mockMvc.perform(get("/api/reports/dashboard/admin")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager dashboard accessible by MANAGER, forbidden for EMPLOYEE")
    void managerDashboard_roleCheck() throws Exception {
        // Manager access -> 200
        mockMvc.perform(get("/api/reports/dashboard/manager")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.departmentName").exists());

        // Employee access -> 403
        mockMvc.perform(get("/api/reports/dashboard/manager")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee dashboard returns authenticated employee's data")
    void employeeDashboard_success() throws Exception {
        mockMvc.perform(get("/api/reports/dashboard/employee")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeId").value(regularEmp.getId()))
                .andExpect(jsonPath("$.data.employeeName").value("Report Developer"));
    }

    @Test
    @DisplayName("Analytics endpoints return 200 for authenticated requests")
    void analyticsEndpoints_return200() throws Exception {
        mockMvc.perform(get("/api/reports/tasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTasks").isNumber());

        mockMvc.perform(get("/api/reports/attendance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRecords").isNumber());

        mockMvc.perform(get("/api/reports/leaves")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLeaveRequests").isNumber());

        mockMvc.perform(get("/api/reports/complaints")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalComplaints").isNumber());
    }
}

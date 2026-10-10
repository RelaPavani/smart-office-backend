package com.smartoffice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoffice.dto.announcement.AnnouncementCreateRequest;
import com.smartoffice.dto.complaint.ComplaintCreateRequest;
import com.smartoffice.dto.meeting.MeetingCreateRequest;
import com.smartoffice.dto.task.TaskCreateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.MeetingStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Prompt5IntegrationTest {

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
    private Employee testEmployee;
    private Department testDept;

    @BeforeEach
    void setupTestData() {
        String adminUsername = "p5_admin_" + System.currentTimeMillis();
        User adminUser = userRepository.save(User.builder()
                .username(adminUsername)
                .email(adminUsername + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
        adminToken = jwtService.generateToken(adminUsername, "ADMIN");

        testDept = departmentRepository.save(Department.builder()
                .name("P5_Dept_" + System.currentTimeMillis())
                .description("Prompt 5 Test Dept")
                .active(true)
                .build());

        String empUsername = "p5_emp_" + System.currentTimeMillis();
        User empUser = userRepository.save(User.builder()
                .username(empUsername)
                .email(empUsername + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build());
        employeeToken = jwtService.generateToken(empUsername, "EMPLOYEE");

        testEmployee = employeeRepository.save(Employee.builder()
                .employeeCode("P5_EMP_" + System.currentTimeMillis())
                .firstName("Prompt5")
                .lastName("Employee")
                .email(empUsername + "@smartoffice.com")
                .designation("Software Engineer")
                .joiningDate(LocalDate.now().minusDays(5))
                .status(EmployeeStatus.ACTIVE)
                .department(testDept)
                .user(empUser)
                .build());
    }

    @Test
    @DisplayName("Protected Prompt 5 endpoints return 401 without JWT")
    void testProtectedEndpointsReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/meetings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/announcements")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/complaints")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin creates task and employee retrieves it via /my")
    void testAdminCreatesTaskAndEmployeeRetrievesViaMy() throws Exception {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Prompt 5 Integration Task")
                .description("Test end-to-end task flow")
                .assignedToId(testEmployee.getId())
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .deadline(LocalDateTime.now().plusDays(2))
                .build();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Prompt 5 Integration Task"));

        mockMvc.perform(get("/api/tasks/my")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Employee cannot create task - returns 403")
    void testEmployeeCannotCreateTask() throws Exception {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Unauthorized Employee Task")
                .assignedToId(testEmployee.getId())
                .priority(TaskPriority.LOW)
                .build();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin schedules meeting and employee views /api/meetings/my")
    void testAdminSchedulesMeeting() throws Exception {
        MeetingCreateRequest request = MeetingCreateRequest.builder()
                .title("Prompt 5 Sprint Review")
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .agenda("Review prompt 5 deliverables")
                .organizerId(testEmployee.getId())
                .status(MeetingStatus.SCHEDULED)
                .build();

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Prompt 5 Sprint Review"));

        mockMvc.perform(get("/api/meetings/my")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Admin publishes announcement and employee views published feed")
    void testAnnouncementFlow() throws Exception {
        AnnouncementCreateRequest request = AnnouncementCreateRequest.builder()
                .title("Prompt 5 System Update")
                .content("New modules deployed.")
                .expiryDate(LocalDate.now().plusDays(7))
                .active(true)
                .build();

        mockMvc.perform(post("/api/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Prompt 5 System Update"));

        mockMvc.perform(get("/api/announcements/published")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Employee creates complaint and views via /my")
    void testComplaintFlow() throws Exception {
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .employeeId(testEmployee.getId())
                .title("Keyboard key sticky")
                .description("Spacebar gets stuck periodically.")
                .category(ComplaintCategory.IT)
                .priority(ComplaintPriority.MEDIUM)
                .build();

        mockMvc.perform(post("/api/complaints")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Keyboard key sticky"));

        mockMvc.perform(get("/api/complaints/my")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}

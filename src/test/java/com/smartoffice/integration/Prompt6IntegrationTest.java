package com.smartoffice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoffice.dto.ai.AssistantChatRequest;
import com.smartoffice.dto.ai.ComplaintClassificationRequest;
import com.smartoffice.dto.ai.MeetingNotesSummaryRequest;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionRequest;
import com.smartoffice.entity.Announcement;
import com.smartoffice.entity.Attendance;
import com.smartoffice.entity.Complaint;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.LeaveBalance;
import com.smartoffice.entity.LeaveRequest;
import com.smartoffice.entity.Meeting;
import com.smartoffice.entity.MeetingParticipant;
import com.smartoffice.entity.Task;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.AttendanceStatus;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.LeaveStatus;
import com.smartoffice.entity.enums.LeaveType;
import com.smartoffice.entity.enums.MeetingStatus;
import com.smartoffice.entity.enums.ParticipantStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
import com.smartoffice.repository.AnnouncementRepository;
import com.smartoffice.repository.AttendanceRepository;
import com.smartoffice.repository.ComplaintRepository;
import com.smartoffice.repository.DepartmentRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.LeaveBalanceRepository;
import com.smartoffice.repository.LeaveRequestRepository;
import com.smartoffice.repository.MeetingParticipantRepository;
import com.smartoffice.repository.MeetingRepository;
import com.smartoffice.repository.TaskRepository;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Prompt6IntegrationTest {

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
    private TaskRepository taskRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository meetingParticipantRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String managerToken;
    private String employeeToken1;
    private String employeeToken2;

    private Department engDept;
    private Department hrDept;
    private Employee managerEmp;
    private Employee engEmp;
    private Employee hrEmp;
    private Meeting teamMeeting;

    @BeforeEach
    void setupPrompt6Data() {
        engDept = departmentRepository.save(Department.builder()
                .name("Engineering_" + System.currentTimeMillis())
                .description("Software Engineering Department")
                .active(true)
                .build());

        hrDept = departmentRepository.save(Department.builder()
                .name("HR_" + System.currentTimeMillis())
                .description("Human Resources Department")
                .active(true)
                .build());

        String adminUser = "p6_admin_" + System.currentTimeMillis();
        userRepository.save(User.builder()
                .username(adminUser)
                .email(adminUser + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
        adminToken = jwtService.generateToken(adminUser, "ADMIN");

        String mgrUser = "p6_mgr_" + System.currentTimeMillis();
        User mu = userRepository.save(User.builder()
                .username(mgrUser)
                .email(mgrUser + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.MANAGER)
                .enabled(true)
                .build());
        managerToken = jwtService.generateToken(mgrUser, "MANAGER");

        managerEmp = employeeRepository.save(Employee.builder()
                .employeeCode("P6_MGR_" + System.currentTimeMillis())
                .firstName("Tech")
                .lastName("Lead")
                .email(mgrUser + "@smartoffice.com")
                .designation("Engineering Manager")
                .joiningDate(LocalDate.now().minusMonths(6))
                .status(EmployeeStatus.ACTIVE)
                .department(engDept)
                .user(mu)
                .build());

        String empUser1 = "p6_eng_" + System.currentTimeMillis();
        User u1 = userRepository.save(User.builder()
                .username(empUser1)
                .email(empUser1 + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build());
        employeeToken1 = jwtService.generateToken(empUser1, "EMPLOYEE");

        engEmp = employeeRepository.save(Employee.builder()
                .employeeCode("P6_ENG_" + System.currentTimeMillis())
                .firstName("Dev")
                .lastName("Engineer")
                .email(empUser1 + "@smartoffice.com")
                .designation("Software Engineer")
                .joiningDate(LocalDate.now().minusMonths(3))
                .status(EmployeeStatus.ACTIVE)
                .department(engDept)
                .manager(managerEmp)
                .user(u1)
                .build());

        String empUser2 = "p6_hr_" + System.currentTimeMillis();
        User u2 = userRepository.save(User.builder()
                .username(empUser2)
                .email(empUser2 + "@smartoffice.com")
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.EMPLOYEE)
                .enabled(true)
                .build());
        employeeToken2 = jwtService.generateToken(empUser2, "EMPLOYEE");

        hrEmp = employeeRepository.save(Employee.builder()
                .employeeCode("P6_HR_" + System.currentTimeMillis())
                .firstName("HR")
                .lastName("Specialist")
                .email(empUser2 + "@smartoffice.com")
                .designation("HR Associate")
                .joiningDate(LocalDate.now().minusMonths(2))
                .status(EmployeeStatus.ACTIVE)
                .department(hrDept)
                .user(u2)
                .build());

        taskRepository.save(Task.builder()
                .title("Prompt 6 AI Architecture")
                .description("Build AI and reporting modules")
                .assignedTo(engEmp)
                .assignedBy(managerEmp)
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.IN_PROGRESS)
                .deadline(LocalDateTime.now().plusDays(3))
                .build());

        taskRepository.save(Task.builder()
                .title("Completed Setup Task")
                .description("Initial environment preparation")
                .assignedTo(engEmp)
                .assignedBy(managerEmp)
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build());

        attendanceRepository.save(Attendance.builder()
                .employee(engEmp)
                .attendanceDate(LocalDate.now())
                .checkIn(LocalDateTime.now().minusHours(4))
                .status(AttendanceStatus.PRESENT)
                .build());

        leaveBalanceRepository.save(LeaveBalance.builder()
                .employee(engEmp)
                .year(LocalDate.now().getYear())
                .leaveType(LeaveType.CASUAL)
                .totalLeaves(14)
                .usedLeaves(2)
                .remainingLeaves(12)
                .build());

        leaveRequestRepository.save(LeaveRequest.builder()
                .employee(engEmp)
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .reason("Personal trip")
                .status(LeaveStatus.PENDING)
                .build());

        complaintRepository.save(Complaint.builder()
                .employee(engEmp)
                .title("Slow development laptop")
                .description("Laptop has 8GB RAM and keeps running out of memory")
                .category(ComplaintCategory.IT)
                .priority(ComplaintPriority.MEDIUM)
                .status(ComplaintStatus.OPEN)
                .build());

        teamMeeting = meetingRepository.save(Meeting.builder()
                .title("Sprint Sync Meeting")
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .agenda("Discuss prompt 6 delivery")
                .organizer(managerEmp)
                .status(MeetingStatus.SCHEDULED)
                .build());

        meetingParticipantRepository.save(MeetingParticipant.builder()
                .meeting(teamMeeting)
                .employee(engEmp)
                .attendanceStatus(ParticipantStatus.ACCEPTED)
                .build());

        announcementRepository.save(Announcement.builder()
                .title("Smart Office Prompt 6 Release Notice")
                .content("AI features and dashboards are now live in backend!")
                .publishedBy(u1)
                .publishedAt(LocalDateTime.now())
                .expiryDate(LocalDate.now().plusMonths(1))
                .active(true)
                .build());
    }

    @Test
    @DisplayName("End-to-end: AI features and dashboards flow")
    void testPrompt6CompleteFlow() throws Exception {
        // 1. Complaint classification
        ComplaintClassificationRequest classifyReq = ComplaintClassificationRequest.builder()
                .title("Payroll discrepancy in monthly bonus")
                .description("Bonus was not credited in this month payslip")
                .build();

        mockMvc.perform(post("/api/ai/complaints/classify")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedCategory").value("PAYROLL"));

        // 2. Task subtask suggestion
        TaskSubtaskSuggestionRequest subtaskReq = TaskSubtaskSuggestionRequest.builder()
                .taskTitle("Implement AI Provider")
                .taskDescription("Build Ollama with fallback")
                .priority(TaskPriority.HIGH)
                .build();

        mockMvc.perform(post("/api/ai/tasks/suggest-subtasks")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subtaskReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedSubtasks").isArray());

        // 3. Employee Performance Summary & Role checks
        // Own summary -> 200
        mockMvc.perform(get("/api/ai/employees/" + engEmp.getId() + "/performance-summary")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedTasks", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.completedTasks", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalAttendanceDays", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.disclaimer").exists());

        // Manager of employee -> 200
        mockMvc.perform(get("/api/ai/employees/" + engEmp.getId() + "/performance-summary")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        // Employee viewing other employee -> 403
        mockMvc.perform(get("/api/ai/employees/" + engEmp.getId() + "/performance-summary")
                        .header("Authorization", "Bearer " + employeeToken2))
                .andExpect(status().isForbidden());

        // Manager viewing employee outside team -> 403
        mockMvc.perform(get("/api/ai/employees/" + hrEmp.getId() + "/performance-summary")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // 4. Meeting Notes Summary
        MeetingNotesSummaryRequest meetReq = MeetingNotesSummaryRequest.builder()
                .meetingId(teamMeeting.getId())
                .notes("Discussion on deliverables. Action: Dev will finalize PR. Todo: Lead will review PR.")
                .build();

        mockMvc.perform(post("/api/ai/meetings/summarize")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actionItems").isArray());

        // 5. Office Assistant Chat
        AssistantChatRequest chatTasks = AssistantChatRequest.builder()
                .message("What are my pending tasks?")
                .build();

        mockMvc.perform(post("/api/ai/assistant/chat")
                        .header("Authorization", "Bearer " + employeeToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatTasks)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("DATABASE"))
                .andExpect(jsonPath("$.data.answer").exists());

        // 6. Admin Dashboard
        mockMvc.perform(get("/api/reports/dashboard/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEmployees", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.totalTasksCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.tasksByPriority").isMap());

        // 7. Manager Dashboard
        mockMvc.perform(get("/api/reports/dashboard/manager")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamSize", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.teamTotalTasks", greaterThanOrEqualTo(2)));

        // 8. Employee Dashboard
        mockMvc.perform(get("/api/reports/dashboard/employee")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeId").value(engEmp.getId()))
                .andExpect(jsonPath("$.data.assignedTasksCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.recentTasks").isArray());

        // 9. Analytics endpoints
        mockMvc.perform(get("/api/reports/tasks")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTasks", greaterThanOrEqualTo(2)));

        mockMvc.perform(get("/api/reports/attendance")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/reports/leaves")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalLeaveRequests", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/reports/complaints")
                        .header("Authorization", "Bearer " + employeeToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalComplaints", greaterThanOrEqualTo(1)));
    }
}

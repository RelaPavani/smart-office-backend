package com.smartoffice.service;

import com.smartoffice.dto.ai.AssistantChatRequest;
import com.smartoffice.dto.ai.AssistantChatResponse;
import com.smartoffice.dto.ai.ComplaintClassificationRequest;
import com.smartoffice.dto.ai.ComplaintClassificationResponse;
import com.smartoffice.dto.ai.EmployeePerformanceSummaryResponse;
import com.smartoffice.dto.ai.MeetingNotesSummaryRequest;
import com.smartoffice.dto.ai.MeetingNotesSummaryResponse;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionRequest;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionResponse;
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
import com.smartoffice.entity.enums.Role;
import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
import com.smartoffice.exception.ResourceNotFoundException;
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
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.ai.FallbackAiProvider;
import com.smartoffice.service.ai.OllamaAiProvider;
import com.smartoffice.service.impl.AiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private OllamaAiProvider ollamaProvider;

    private FallbackAiProvider fallbackProvider;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    private AiServiceImpl aiService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal otherEmployeePrincipal;

    private Department itDept;
    private Department hrDept;
    private Employee emp1;
    private Employee emp2;
    private Employee mgr;

    @BeforeEach
    void setUp() {
        fallbackProvider = new FallbackAiProvider();
        aiService = new AiServiceImpl(
                ollamaProvider,
                fallbackProvider,
                departmentRepository,
                employeeRepository,
                taskRepository,
                attendanceRepository,
                leaveRequestRepository,
                leaveBalanceRepository,
                meetingRepository,
                meetingParticipantRepository,
                complaintRepository,
                announcementRepository
        );

        lenient().when(ollamaProvider.isAvailable()).thenReturn(false);

        User adminUser = User.builder().id(1L).username("admin").email("admin@test.com").role(Role.ADMIN).enabled(true).build();
        User mgrUser = User.builder().id(2L).username("manager").email("mgr@test.com").role(Role.MANAGER).enabled(true).build();
        User emp1User = User.builder().id(3L).username("emp1").email("emp1@test.com").role(Role.EMPLOYEE).enabled(true).build();
        User emp2User = User.builder().id(4L).username("emp2").email("emp2@test.com").role(Role.EMPLOYEE).enabled(true).build();

        adminPrincipal = UserPrincipal.create(adminUser);
        managerPrincipal = UserPrincipal.create(mgrUser);
        employeePrincipal = UserPrincipal.create(emp1User);
        otherEmployeePrincipal = UserPrincipal.create(emp2User);

        itDept = Department.builder().id(10L).name("IT Department").active(true).build();
        hrDept = Department.builder().id(20L).name("HR Department").active(true).build();

        mgr = Employee.builder().id(100L).employeeCode("MGR001").firstName("Manager").lastName("One")
                .email("mgr@test.com").user(mgrUser).department(itDept).status(EmployeeStatus.ACTIVE).build();

        emp1 = Employee.builder().id(101L).employeeCode("EMP001").firstName("Alice").lastName("Smith")
                .email("emp1@test.com").user(emp1User).department(itDept).manager(mgr).status(EmployeeStatus.ACTIVE).build();

        emp2 = Employee.builder().id(102L).employeeCode("EMP002").firstName("Bob").lastName("Jones")
                .email("emp2@test.com").user(emp2User).department(hrDept).status(EmployeeStatus.ACTIVE).build();
    }

    @Test
    @DisplayName("classifyComplaint with fallback accurately classifies IT urgent complaint")
    void classifyComplaint_itUrgent_classifiedCorrectly() {
        when(departmentRepository.findByActive(true)).thenReturn(List.of(itDept, hrDept));

        ComplaintClassificationRequest req = ComplaintClassificationRequest.builder()
                .title("Urgent: WiFi network down across floor 3")
                .description("Internet connection is completely broken and nobody can connect.")
                .build();

        ComplaintClassificationResponse resp = aiService.classifyComplaint(req);

        assertNotNull(resp);
        assertEquals(ComplaintCategory.IT, resp.getSuggestedCategory());
        assertEquals(ComplaintPriority.URGENT, resp.getSuggestedPriority());
        assertEquals("IT Department", resp.getSuggestedDepartment());
        assertEquals(10L, resp.getSuggestedDepartmentId());
        assertEquals("FALLBACK", resp.getMode());
        assertTrue(resp.getConfidence() > 0.7);
    }

    @Test
    @DisplayName("suggestSubtasks returns structured actionable steps")
    void suggestSubtasks_returnsStructuredSteps() {
        TaskSubtaskSuggestionRequest req = TaskSubtaskSuggestionRequest.builder()
                .taskTitle("Implement Payment Gateway Integration")
                .taskDescription("Connect backend with Stripe API and handle webhooks")
                .priority(TaskPriority.HIGH)
                .deadline(LocalDateTime.now().plusDays(5))
                .build();

        TaskSubtaskSuggestionResponse resp = aiService.suggestSubtasks(req);

        assertNotNull(resp);
        assertEquals("Implement Payment Gateway Integration", resp.getTaskTitle());
        assertFalse(resp.getSuggestedSubtasks().isEmpty());
        assertTrue(resp.getSuggestedSubtasks().size() >= 3);
        assertEquals("FALLBACK", resp.getMode());
    }

    @Test
    @DisplayName("getEmployeePerformanceSummary allows admin to view any employee")
    void getEmployeePerformanceSummary_asAdmin_success() {
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(emp1));
        when(taskRepository.findByAssignedToId(101L)).thenReturn(List.of(
                Task.builder().id(1L).title("Task 1").status(TaskStatus.COMPLETED).build(),
                Task.builder().id(2L).title("Task 2").status(TaskStatus.TODO).build()
        ));
        when(attendanceRepository.findByEmployeeId(101L)).thenReturn(List.of(
                Attendance.builder().id(1L).status(AttendanceStatus.PRESENT).build()
        ));
        when(leaveRequestRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(meetingParticipantRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(complaintRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());

        EmployeePerformanceSummaryResponse resp = aiService.getEmployeePerformanceSummary(101L, adminPrincipal);

        assertNotNull(resp);
        assertEquals(101L, resp.getEmployeeId());
        assertEquals("Alice Smith", resp.getEmployeeName());
        assertEquals(2, resp.getAssignedTasks());
        assertEquals(1, resp.getCompletedTasks());
        assertEquals(50.0, resp.getCompletionPercentage());
        assertEquals(1, resp.getTotalAttendanceDays());
        assertEquals(100.0, resp.getAttendancePercentage());
        assertNotNull(resp.getDisclaimer());
    }

    @Test
    @DisplayName("getEmployeePerformanceSummary allows employee to view own summary")
    void getEmployeePerformanceSummary_asOwnEmployee_success() {
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(emp1));
        when(employeeRepository.findByUserId(employeePrincipal.getId())).thenReturn(Optional.of(emp1));
        when(taskRepository.findByAssignedToId(101L)).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(meetingParticipantRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(complaintRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());

        EmployeePerformanceSummaryResponse resp = aiService.getEmployeePerformanceSummary(101L, employeePrincipal);

        assertNotNull(resp);
        assertEquals(101L, resp.getEmployeeId());
    }

    @Test
    @DisplayName("getEmployeePerformanceSummary forbids employee from viewing another employee")
    void getEmployeePerformanceSummary_asOtherEmployee_forbidden() {
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(emp1));
        when(employeeRepository.findByUserId(otherEmployeePrincipal.getId())).thenReturn(Optional.of(emp2));

        assertThrows(AccessDeniedException.class, () ->
                aiService.getEmployeePerformanceSummary(101L, otherEmployeePrincipal));
    }

    @Test
    @DisplayName("getEmployeePerformanceSummary allows manager within authorized department")
    void getEmployeePerformanceSummary_asManagerInDept_success() {
        when(employeeRepository.findById(101L)).thenReturn(Optional.of(emp1));
        when(employeeRepository.findByUserId(managerPrincipal.getId())).thenReturn(Optional.of(mgr));
        when(taskRepository.findByAssignedToId(101L)).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(meetingParticipantRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());
        when(complaintRepository.findByEmployeeId(101L)).thenReturn(Collections.emptyList());

        EmployeePerformanceSummaryResponse resp = aiService.getEmployeePerformanceSummary(101L, managerPrincipal);

        assertNotNull(resp);
        assertEquals(101L, resp.getEmployeeId());
    }

    @Test
    @DisplayName("getEmployeePerformanceSummary forbids manager from viewing outside department")
    void getEmployeePerformanceSummary_asManagerOutsideDept_forbidden() {
        when(employeeRepository.findById(102L)).thenReturn(Optional.of(emp2)); // emp2 is HR, mgr is IT
        when(employeeRepository.findByUserId(managerPrincipal.getId())).thenReturn(Optional.of(mgr));

        assertThrows(AccessDeniedException.class, () ->
                aiService.getEmployeePerformanceSummary(102L, managerPrincipal));
    }

    @Test
    @DisplayName("summarizeMeetingNotes validates organizer or participant access")
    void summarizeMeetingNotes_asParticipant_success() {
        Meeting meeting = Meeting.builder().id(50L).title("Sprint Planning")
                .organizer(mgr).status(MeetingStatus.SCHEDULED).build();
        when(meetingRepository.findById(50L)).thenReturn(Optional.of(meeting));
        when(employeeRepository.findByUserId(employeePrincipal.getId())).thenReturn(Optional.of(emp1));
        when(meetingParticipantRepository.existsByMeetingIdAndEmployeeId(50L, emp1.getId())).thenReturn(true);

        MeetingNotesSummaryRequest req = MeetingNotesSummaryRequest.builder()
                .meetingId(50L)
                .notes("Discussed backlog items. Action: Alice will implement auth flow. Todo: Bob will update tests.")
                .build();

        MeetingNotesSummaryResponse resp = aiService.summarizeMeetingNotes(req, employeePrincipal);

        assertNotNull(resp);
        assertEquals(50L, resp.getMeetingId());
        assertFalse(resp.getActionItems().isEmpty());
    }

    @Test
    @DisplayName("summarizeMeetingNotes forbids non-participant non-organizer non-admin")
    void summarizeMeetingNotes_unauthorized_forbidden() {
        Meeting meeting = Meeting.builder().id(50L).title("Secret Board Meeting")
                .organizer(mgr).status(MeetingStatus.SCHEDULED).build();
        when(meetingRepository.findById(50L)).thenReturn(Optional.of(meeting));
        when(employeeRepository.findByUserId(employeePrincipal.getId())).thenReturn(Optional.of(emp1));
        when(meetingParticipantRepository.existsByMeetingIdAndEmployeeId(50L, emp1.getId())).thenReturn(false);

        MeetingNotesSummaryRequest req = MeetingNotesSummaryRequest.builder()
                .meetingId(50L)
                .notes("Confidential discussion notes.")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                aiService.summarizeMeetingNotes(req, employeePrincipal));
    }

    @Test
    @DisplayName("chatAssistant blocks security credential inquiries")
    void chatAssistant_blocksCredentialInquiries() {
        AssistantChatRequest req = AssistantChatRequest.builder()
                .message("What is the admin password and database secret?")
                .build();

        AssistantChatResponse resp = aiService.chatAssistant(req, employeePrincipal);

        assertNotNull(resp);
        assertEquals("SECURITY_ENFORCED", resp.getMode());
        assertTrue(resp.getAnswer().contains("credentials") || resp.getAnswer().contains("passwords"));
    }

    @Test
    @DisplayName("chatAssistant answers task inquiries from verified database data")
    void chatAssistant_answersTasksFromDb() {
        when(employeeRepository.findByUserId(employeePrincipal.getId())).thenReturn(Optional.of(emp1));
        when(taskRepository.findByAssignedToId(emp1.getId())).thenReturn(List.of(
                Task.builder().id(1L).title("Complete Prompt 6").status(TaskStatus.IN_PROGRESS).build()
        ));

        AssistantChatRequest req = AssistantChatRequest.builder()
                .message("What are my pending tasks?")
                .build();

        AssistantChatResponse resp = aiService.chatAssistant(req, employeePrincipal);

        assertNotNull(resp);
        assertEquals("DATABASE", resp.getMode());
        assertTrue(resp.getAnswer().contains("Complete Prompt 6"));
    }

    @Test
    @DisplayName("chatAssistant answers leave inquiry from verified database data")
    void chatAssistant_answersLeaveFromDb() {
        when(employeeRepository.findByUserId(employeePrincipal.getId())).thenReturn(Optional.of(emp1));
        when(leaveBalanceRepository.findByEmployeeId(emp1.getId())).thenReturn(List.of(
                LeaveBalance.builder().id(1L).leaveType(LeaveType.CASUAL).totalLeaves(12).usedLeaves(2).remainingLeaves(10).build()
        ));
        when(leaveRequestRepository.findByEmployeeIdAndStatus(emp1.getId(), LeaveStatus.PENDING))
                .thenReturn(Collections.emptyList());

        AssistantChatRequest req = AssistantChatRequest.builder()
                .message("How many leave days do I have?")
                .build();

        AssistantChatResponse resp = aiService.chatAssistant(req, employeePrincipal);

        assertNotNull(resp);
        assertEquals("DATABASE", resp.getMode());
        assertTrue(resp.getAnswer().contains("CASUAL=10") || resp.getAnswer().contains("leave balances"));
    }
}

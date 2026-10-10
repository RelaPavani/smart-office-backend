package com.smartoffice.service;

import com.smartoffice.dto.report.AdminDashboardResponse;
import com.smartoffice.dto.report.AttendanceAnalyticsResponse;
import com.smartoffice.dto.report.ComplaintAnalyticsResponse;
import com.smartoffice.dto.report.EmployeeDashboardResponse;
import com.smartoffice.dto.report.LeaveAnalyticsResponse;
import com.smartoffice.dto.report.ManagerDashboardResponse;
import com.smartoffice.dto.report.TaskAnalyticsResponse;
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
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.ReportServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal employeePrincipal;

    private Department itDept;
    private Department hrDept;
    private Employee mgr;
    private Employee emp1;

    @BeforeEach
    void setUp() {
        User adminUser = User.builder().id(1L).username("admin").email("admin@test.com").role(Role.ADMIN).enabled(true).build();
        User mgrUser = User.builder().id(2L).username("manager").email("mgr@test.com").role(Role.MANAGER).enabled(true).build();
        User empUser = User.builder().id(3L).username("emp1").email("emp1@test.com").role(Role.EMPLOYEE).enabled(true).build();

        adminPrincipal = UserPrincipal.create(adminUser);
        managerPrincipal = UserPrincipal.create(mgrUser);
        employeePrincipal = UserPrincipal.create(empUser);

        itDept = Department.builder().id(10L).name("IT").active(true).build();
        hrDept = Department.builder().id(20L).name("HR").active(true).build();

        mgr = Employee.builder().id(100L).employeeCode("MGR01").firstName("Manager").lastName("IT")
                .email("mgr@test.com").department(itDept).user(mgrUser).status(EmployeeStatus.ACTIVE).build();

        emp1 = Employee.builder().id(101L).employeeCode("EMP01").firstName("Alice").lastName("Wonder")
                .email("emp1@test.com").department(itDept).manager(mgr).user(empUser).status(EmployeeStatus.ACTIVE).build();
    }

    @Test
    @DisplayName("getTaskAnalytics as admin returns all tasks")
    void getTaskAnalytics_asAdmin_success() {
        when(taskRepository.findAll()).thenReturn(List.of(
                Task.builder().id(1L).title("T1").status(TaskStatus.COMPLETED).priority(TaskPriority.HIGH).build(),
                Task.builder().id(2L).title("T2").status(TaskStatus.TODO).priority(TaskPriority.MEDIUM).build()
        ));

        TaskAnalyticsResponse resp = reportService.getTaskAnalytics(null, null, adminPrincipal);

        assertNotNull(resp);
        assertEquals(2, resp.getTotalTasks());
        assertEquals(1, resp.getCompletedTasks());
        assertEquals(1, resp.getTodoTasks());
        assertEquals(50.0, resp.getCompletionRate());
        assertEquals(1L, resp.getTasksByPriority().get("HIGH"));
    }

    @Test
    @DisplayName("getTaskAnalytics as manager scopes to manager department")
    void getTaskAnalytics_asManager_scopesToDept() {
        when(employeeRepository.findByUserId(managerPrincipal.getId())).thenReturn(Optional.of(mgr));
        when(taskRepository.findByAssignedToDepartmentId(itDept.getId())).thenReturn(List.of(
                Task.builder().id(1L).title("T1").status(TaskStatus.COMPLETED).priority(TaskPriority.LOW).build()
        ));

        TaskAnalyticsResponse resp = reportService.getTaskAnalytics(null, null, managerPrincipal);

        assertNotNull(resp);
        assertEquals(1, resp.getTotalTasks());
        assertEquals(100.0, resp.getCompletionRate());
    }

    @Test
    @DisplayName("getTaskAnalytics as manager forbids accessing another department")
    void getTaskAnalytics_asManager_outsideDept_forbidden() {
        when(employeeRepository.findByUserId(managerPrincipal.getId())).thenReturn(Optional.of(mgr));

        assertThrows(AccessDeniedException.class, () ->
                reportService.getTaskAnalytics(hrDept.getId(), null, managerPrincipal));
    }

    @Test
    @DisplayName("getTaskAnalytics as employee forbids department-wide query")
    void getTaskAnalytics_asEmployee_deptQuery_forbidden() {
        when(employeeRepository.findByUserId(employeePrincipal.getId())).thenReturn(Optional.of(emp1));

        assertThrows(AccessDeniedException.class, () ->
                reportService.getTaskAnalytics(itDept.getId(), null, employeePrincipal));
    }

    @Test
    @DisplayName("getAttendanceAnalytics calculates rates and breakdowns")
    void getAttendanceAnalytics_asAdmin_success() {
        when(attendanceRepository.findAll()).thenReturn(List.of(
                Attendance.builder().id(1L).status(AttendanceStatus.PRESENT).build(),
                Attendance.builder().id(2L).status(AttendanceStatus.LATE).build(),
                Attendance.builder().id(3L).status(AttendanceStatus.ABSENT).build()
        ));

        AttendanceAnalyticsResponse resp = reportService.getAttendanceAnalytics(null, null, null, null, adminPrincipal);

        assertNotNull(resp);
        assertEquals(3, resp.getTotalRecords());
        assertEquals(1, resp.getPresentCount());
        assertEquals(1, resp.getLateCount());
        assertEquals(1, resp.getAbsentCount());
        assertEquals(33.3, resp.getOverallAttendanceRate());
    }

    @Test
    @DisplayName("getLeaveAnalytics calculates approval rate and status breakdown")
    void getLeaveAnalytics_asAdmin_success() {
        when(leaveRequestRepository.findAll()).thenReturn(List.of(
                LeaveRequest.builder().id(1L).status(LeaveStatus.APPROVED).leaveType(LeaveType.CASUAL).build(),
                LeaveRequest.builder().id(2L).status(LeaveStatus.PENDING).leaveType(LeaveType.SICK).build()
        ));

        LeaveAnalyticsResponse resp = reportService.getLeaveAnalytics(null, null, null, null, adminPrincipal);

        assertNotNull(resp);
        assertEquals(2, resp.getTotalLeaveRequests());
        assertEquals(1, resp.getApprovedLeaves());
        assertEquals(1, resp.getPendingLeaves());
        assertEquals(50.0, resp.getApprovalRate());
    }

    @Test
    @DisplayName("getComplaintAnalytics calculates resolution rate and category breakdown")
    void getComplaintAnalytics_asAdmin_success() {
        when(complaintRepository.findAll()).thenReturn(List.of(
                Complaint.builder().id(1L).status(ComplaintStatus.RESOLVED).category(ComplaintCategory.IT).priority(ComplaintPriority.HIGH).build(),
                Complaint.builder().id(2L).status(ComplaintStatus.OPEN).category(ComplaintCategory.HR).priority(ComplaintPriority.LOW).build()
        ));

        ComplaintAnalyticsResponse resp = reportService.getComplaintAnalytics(null, null, adminPrincipal);

        assertNotNull(resp);
        assertEquals(2, resp.getTotalComplaints());
        assertEquals(1, resp.getResolvedComplaints());
        assertEquals(1, resp.getOpenComplaints());
        assertEquals(50.0, resp.getResolutionRate());
    }

    @Test
    @DisplayName("getAdminDashboard succeeds for admin and populates all fields")
    void getAdminDashboard_asAdmin_success() {
        when(employeeRepository.count()).thenReturn(10L);
        when(employeeRepository.findByStatus(EmployeeStatus.ACTIVE)).thenReturn(List.of(emp1, mgr));
        when(departmentRepository.count()).thenReturn(2L);
        when(attendanceRepository.findByAttendanceDate(LocalDate.now())).thenReturn(List.of(
                Attendance.builder().id(1L).status(AttendanceStatus.PRESENT).build()
        ));
        when(leaveRequestRepository.findByStatus(LeaveStatus.PENDING)).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findByStatus(LeaveStatus.APPROVED)).thenReturn(Collections.emptyList());
        when(leaveRequestRepository.findByStatus(LeaveStatus.REJECTED)).thenReturn(Collections.emptyList());
        when(taskRepository.findAll()).thenReturn(Collections.emptyList());
        when(meetingRepository.count()).thenReturn(5L);
        when(meetingRepository.findByStartDateTimeGreaterThanEqual(any())).thenReturn(Collections.emptyList());
        when(complaintRepository.findByStatus(ComplaintStatus.OPEN)).thenReturn(Collections.emptyList());
        when(complaintRepository.findByStatus(ComplaintStatus.IN_PROGRESS)).thenReturn(Collections.emptyList());
        when(complaintRepository.findByStatus(ComplaintStatus.RESOLVED)).thenReturn(Collections.emptyList());
        when(announcementRepository.findByActive(true)).thenReturn(Collections.emptyList());
        when(complaintRepository.findAll()).thenReturn(Collections.emptyList());
        when(departmentRepository.findByActive(true)).thenReturn(List.of(itDept));
        when(employeeRepository.findByDepartmentIdAndStatus(itDept.getId(), EmployeeStatus.ACTIVE)).thenReturn(List.of(emp1));

        AdminDashboardResponse resp = reportService.getAdminDashboard(adminPrincipal);

        assertNotNull(resp);
        assertEquals(10L, resp.getTotalEmployees());
        assertEquals(2L, resp.getActiveEmployees());
        assertEquals(2L, resp.getTotalDepartments());
        assertEquals(1L, resp.getTodayPresentCount());
        assertEquals(50.0, resp.getTodayAttendanceRate());
    }

    @Test
    @DisplayName("getAdminDashboard forbids non-admin users")
    void getAdminDashboard_asEmployee_forbidden() {
        assertThrows(AccessDeniedException.class, () ->
                reportService.getAdminDashboard(employeePrincipal));
    }

    @Test
    @DisplayName("getManagerDashboard succeeds for manager and scopes to team")
    void getManagerDashboard_asManager_success() {
        when(employeeRepository.findByUserId(managerPrincipal.getId())).thenReturn(Optional.of(mgr));
        when(employeeRepository.findByDepartmentId(itDept.getId())).thenReturn(List.of(mgr, emp1));
        when(attendanceRepository.findByEmployeeDepartmentIdAndAttendanceDate(itDept.getId(), LocalDate.now()))
                .thenReturn(List.of(Attendance.builder().id(1L).status(AttendanceStatus.PRESENT).build()));
        when(taskRepository.findByAssignedToDepartmentId(itDept.getId())).thenReturn(List.of(
                Task.builder().id(1L).status(TaskStatus.COMPLETED).build()
        ));
        when(leaveRequestRepository.findByEmployeeDepartmentIdAndStatus(itDept.getId(), LeaveStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(complaintRepository.findByEmployeeDepartmentId(itDept.getId())).thenReturn(Collections.emptyList());
        when(meetingParticipantRepository.findByEmployeeIdAndMeetingStartDateTimeGreaterThanEqual(any(), any()))
                .thenReturn(Collections.emptyList());

        ManagerDashboardResponse resp = reportService.getManagerDashboard(managerPrincipal);

        assertNotNull(resp);
        assertEquals(mgr.getId(), resp.getManagerId());
        assertEquals("IT", resp.getDepartmentName());
        assertEquals(2, resp.getTeamSize());
        assertEquals(1, resp.getTodayTeamPresentCount());
        assertEquals(50.0, resp.getTodayTeamAttendanceRate());
        assertEquals(1, resp.getTeamTotalTasks());
        assertEquals(100.0, resp.getTeamTaskCompletionRate());
    }

    @Test
    @DisplayName("getManagerDashboard forbids regular employee")
    void getManagerDashboard_asEmployee_forbidden() {
        assertThrows(AccessDeniedException.class, () ->
                reportService.getManagerDashboard(employeePrincipal));
    }

    @Test
    @DisplayName("getEmployeeDashboard populates authenticated employee stats and isolated data")
    void getEmployeeDashboard_asEmployee_success() {
        when(employeeRepository.findByUserId(employeePrincipal.getId())).thenReturn(Optional.of(emp1));
        when(attendanceRepository.findByEmployeeId(emp1.getId())).thenReturn(List.of(
                Attendance.builder().id(1L).status(AttendanceStatus.PRESENT).build()
        ));
        when(leaveBalanceRepository.findByEmployeeId(emp1.getId())).thenReturn(List.of(
                LeaveBalance.builder().id(1L).leaveType(LeaveType.CASUAL).totalLeaves(12).usedLeaves(2).remainingLeaves(10).build()
        ));
        when(leaveRequestRepository.findByEmployeeIdAndStatus(emp1.getId(), LeaveStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(taskRepository.findByAssignedToId(emp1.getId())).thenReturn(List.of(
                Task.builder().id(1L).title("Test Task").status(TaskStatus.TODO).build()
        ));
        when(meetingParticipantRepository.findByEmployeeIdAndMeetingStartDateTimeGreaterThanEqual(any(), any()))
                .thenReturn(Collections.emptyList());
        when(complaintRepository.findByEmployeeId(emp1.getId())).thenReturn(Collections.emptyList());
        when(announcementRepository.findByActive(true)).thenReturn(Collections.emptyList());

        EmployeeDashboardResponse resp = reportService.getEmployeeDashboard(employeePrincipal);

        assertNotNull(resp);
        assertEquals(emp1.getId(), resp.getEmployeeId());
        assertEquals("Alice Wonder", resp.getEmployeeName());
        assertEquals(1, resp.getTotalAttendanceDays());
        assertEquals(100.0, resp.getAttendancePercentage());
        assertEquals(10, resp.getLeaveBalances().get("CASUAL"));
        assertEquals(1, resp.getAssignedTasksCount());
        assertEquals(1, resp.getPendingTasksCount());
        assertFalse(resp.getRecentTasks().isEmpty());
    }
}

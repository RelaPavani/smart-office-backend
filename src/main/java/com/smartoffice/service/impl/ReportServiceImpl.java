package com.smartoffice.service.impl;

import com.smartoffice.dto.meeting.MeetingResponse;
import com.smartoffice.dto.report.AdminDashboardResponse;
import com.smartoffice.dto.report.AttendanceAnalyticsResponse;
import com.smartoffice.dto.report.ComplaintAnalyticsResponse;
import com.smartoffice.dto.report.EmployeeDashboardResponse;
import com.smartoffice.dto.report.LeaveAnalyticsResponse;
import com.smartoffice.dto.report.ManagerDashboardResponse;
import com.smartoffice.dto.report.TaskAnalyticsResponse;
import com.smartoffice.dto.task.TaskResponse;
import com.smartoffice.entity.Attendance;
import com.smartoffice.entity.Complaint;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.LeaveBalance;
import com.smartoffice.entity.LeaveRequest;
import com.smartoffice.entity.Meeting;
import com.smartoffice.entity.MeetingParticipant;
import com.smartoffice.entity.Task;
import com.smartoffice.entity.enums.AttendanceStatus;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.LeaveStatus;
import com.smartoffice.entity.enums.LeaveType;
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
import com.smartoffice.service.ReportService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final ComplaintRepository complaintRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final AnnouncementRepository announcementRepository;

    public ReportServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            TaskRepository taskRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            ComplaintRepository complaintRepository,
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            AnnouncementRepository announcementRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.taskRepository = taskRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.complaintRepository = complaintRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.announcementRepository = announcementRepository;
    }

    @Override
    public TaskAnalyticsResponse getTaskAnalytics(Long departmentId, Long employeeId, UserPrincipal currentUser) {
        ScopeFilter scope = resolveScope(departmentId, employeeId, currentUser, "task");

        List<Task> tasks;
        if (scope.employeeId() != null) {
            tasks = taskRepository.findByAssignedToId(scope.employeeId());
        } else if (scope.departmentId() != null) {
            tasks = taskRepository.findByAssignedToDepartmentId(scope.departmentId());
        } else {
            tasks = taskRepository.findAll();
        }

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long inProgressTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long todoTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long cancelledTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.CANCELLED).count();
        long overdueTasks = tasks.stream().filter(t ->
                t.getDeadline() != null &&
                        t.getDeadline().isBefore(LocalDateTime.now()) &&
                        t.getStatus() != TaskStatus.COMPLETED &&
                        t.getStatus() != TaskStatus.CANCELLED
        ).count();
        double completionRate = totalTasks > 0 ? Math.round((double) completedTasks / totalTasks * 1000.0) / 10.0 : 0.0;

        Map<String, Long> tasksByPriority = new LinkedHashMap<>();
        for (TaskPriority p : TaskPriority.values()) {
            tasksByPriority.put(p.name(), tasks.stream().filter(t -> t.getPriority() == p).count());
        }

        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            tasksByStatus.put(s.name(), tasks.stream().filter(t -> t.getStatus() == s).count());
        }

        return TaskAnalyticsResponse.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .todoTasks(todoTasks)
                .cancelledTasks(cancelledTasks)
                .overdueTasks(overdueTasks)
                .completionRate(completionRate)
                .tasksByPriority(tasksByPriority)
                .tasksByStatus(tasksByStatus)
                .build();
    }

    @Override
    public AttendanceAnalyticsResponse getAttendanceAnalytics(LocalDate startDate, LocalDate endDate, Long departmentId, Long employeeId, UserPrincipal currentUser) {
        ScopeFilter scope = resolveScope(departmentId, employeeId, currentUser, "attendance");

        List<Attendance> attendances;
        if (scope.employeeId() != null) {
            if (startDate != null && endDate != null) {
                attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(scope.employeeId(), startDate, endDate);
            } else {
                attendances = attendanceRepository.findByEmployeeId(scope.employeeId());
            }
        } else if (scope.departmentId() != null) {
            if (startDate != null && endDate != null) {
                attendances = attendanceRepository.findByEmployeeDepartmentIdAndAttendanceDateBetween(scope.departmentId(), startDate, endDate);
            } else {
                attendances = attendanceRepository.findByEmployeeDepartmentId(scope.departmentId());
            }
        } else {
            if (startDate != null && endDate != null) {
                attendances = attendanceRepository.findByAttendanceDateBetween(startDate, endDate);
            } else {
                attendances = attendanceRepository.findAll();
            }
        }

        long totalRecords = attendances.size();
        long presentCount = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long lateCount = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long halfDayCount = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.HALF_DAY).count();
        long absentCount = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        double overallAttendanceRate = totalRecords > 0 ? Math.round((double) presentCount / totalRecords * 1000.0) / 10.0 : 0.0;

        Map<String, Long> attendanceByStatus = new LinkedHashMap<>();
        for (AttendanceStatus s : AttendanceStatus.values()) {
            attendanceByStatus.put(s.name(), attendances.stream().filter(a -> a.getStatus() == s).count());
        }

        return AttendanceAnalyticsResponse.builder()
                .totalRecords(totalRecords)
                .presentCount(presentCount)
                .lateCount(lateCount)
                .halfDayCount(halfDayCount)
                .absentCount(absentCount)
                .overallAttendanceRate(overallAttendanceRate)
                .attendanceByStatus(attendanceByStatus)
                .build();
    }

    @Override
    public LeaveAnalyticsResponse getLeaveAnalytics(LocalDate startDate, LocalDate endDate, Long departmentId, Long employeeId, UserPrincipal currentUser) {
        ScopeFilter scope = resolveScope(departmentId, employeeId, currentUser, "leave");

        List<LeaveRequest> leaves;
        if (scope.employeeId() != null) {
            if (startDate != null && endDate != null) {
                leaves = leaveRequestRepository.findByEmployeeIdAndStartDateBetween(scope.employeeId(), startDate, endDate);
            } else {
                leaves = leaveRequestRepository.findByEmployeeId(scope.employeeId());
            }
        } else if (scope.departmentId() != null) {
            if (startDate != null && endDate != null) {
                leaves = leaveRequestRepository.findByEmployeeDepartmentIdAndStartDateBetween(scope.departmentId(), startDate, endDate);
            } else {
                leaves = leaveRequestRepository.findByEmployeeDepartmentId(scope.departmentId());
            }
        } else {
            if (startDate != null && endDate != null) {
                leaves = leaveRequestRepository.findByStartDateBetween(startDate, endDate);
            } else {
                leaves = leaveRequestRepository.findAll();
            }
        }

        long totalLeaveRequests = leaves.size();
        long pendingLeaves = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
        long approvedLeaves = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
        long rejectedLeaves = leaves.stream().filter(l -> l.getStatus() == LeaveStatus.REJECTED).count();
        double approvalRate = totalLeaveRequests > 0 ? Math.round((double) approvedLeaves / totalLeaveRequests * 1000.0) / 10.0 : 0.0;

        Map<String, Long> leavesByType = new LinkedHashMap<>();
        for (LeaveType t : LeaveType.values()) {
            leavesByType.put(t.name(), leaves.stream().filter(l -> l.getLeaveType() == t).count());
        }

        Map<String, Long> leavesByStatus = new LinkedHashMap<>();
        for (LeaveStatus s : LeaveStatus.values()) {
            leavesByStatus.put(s.name(), leaves.stream().filter(l -> l.getStatus() == s).count());
        }

        return LeaveAnalyticsResponse.builder()
                .totalLeaveRequests(totalLeaveRequests)
                .pendingLeaves(pendingLeaves)
                .approvedLeaves(approvedLeaves)
                .rejectedLeaves(rejectedLeaves)
                .approvalRate(approvalRate)
                .leavesByType(leavesByType)
                .leavesByStatus(leavesByStatus)
                .build();
    }

    @Override
    public ComplaintAnalyticsResponse getComplaintAnalytics(Long departmentId, Long employeeId, UserPrincipal currentUser) {
        ScopeFilter scope = resolveScope(departmentId, employeeId, currentUser, "complaint");

        List<Complaint> complaints;
        if (scope.employeeId() != null) {
            complaints = complaintRepository.findByEmployeeId(scope.employeeId());
        } else if (scope.departmentId() != null) {
            complaints = complaintRepository.findByEmployeeDepartmentId(scope.departmentId());
        } else {
            complaints = complaintRepository.findAll();
        }

        long totalComplaints = complaints.size();
        long openComplaints = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.OPEN).count();
        long inProgressComplaints = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS).count();
        long resolvedComplaints = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        long closedComplaints = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.CLOSED).count();
        double resolutionRate = totalComplaints > 0 ?
                Math.round((double) (resolvedComplaints + closedComplaints) / totalComplaints * 1000.0) / 10.0 : 0.0;

        Map<String, Long> complaintsByCategory = new LinkedHashMap<>();
        for (ComplaintCategory c : ComplaintCategory.values()) {
            complaintsByCategory.put(c.name(), complaints.stream().filter(item -> item.getCategory() == c).count());
        }

        Map<String, Long> complaintsByPriority = new LinkedHashMap<>();
        for (ComplaintPriority p : ComplaintPriority.values()) {
            complaintsByPriority.put(p.name(), complaints.stream().filter(item -> item.getPriority() == p).count());
        }

        Map<String, Long> complaintsByStatus = new LinkedHashMap<>();
        for (ComplaintStatus s : ComplaintStatus.values()) {
            complaintsByStatus.put(s.name(), complaints.stream().filter(item -> item.getStatus() == s).count());
        }

        return ComplaintAnalyticsResponse.builder()
                .totalComplaints(totalComplaints)
                .openComplaints(openComplaints)
                .inProgressComplaints(inProgressComplaints)
                .resolvedComplaints(resolvedComplaints)
                .closedComplaints(closedComplaints)
                .resolutionRate(resolutionRate)
                .complaintsByCategory(complaintsByCategory)
                .complaintsByPriority(complaintsByPriority)
                .complaintsByStatus(complaintsByStatus)
                .build();
    }

    @Override
    public AdminDashboardResponse getAdminDashboard(UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can access the admin dashboard");
        }

        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE).size();
        long totalDepartments = departmentRepository.count();

        List<Attendance> todayAttendances = attendanceRepository.findByAttendanceDate(LocalDate.now());
        long todayAttendanceCount = todayAttendances.size();
        long todayPresentCount = todayAttendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long todayLateCount = todayAttendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        double todayAttendanceRate = activeEmployees > 0 ?
                Math.round((double) todayPresentCount / activeEmployees * 1000.0) / 10.0 : 0.0;

        long pendingLeavesCount = leaveRequestRepository.findByStatus(LeaveStatus.PENDING).size();
        long approvedLeavesCount = leaveRequestRepository.findByStatus(LeaveStatus.APPROVED).size();
        long rejectedLeavesCount = leaveRequestRepository.findByStatus(LeaveStatus.REJECTED).size();

        List<Task> allTasks = taskRepository.findAll();
        long totalTasksCount = allTasks.size();
        long completedTasksCount = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long pendingTasksCount = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long overdueTasksCount = allTasks.stream().filter(t ->
                t.getDeadline() != null &&
                        t.getDeadline().isBefore(LocalDateTime.now()) &&
                        t.getStatus() != TaskStatus.COMPLETED &&
                        t.getStatus() != TaskStatus.CANCELLED
        ).count();
        double taskCompletionRate = totalTasksCount > 0 ?
                Math.round((double) completedTasksCount / totalTasksCount * 1000.0) / 10.0 : 0.0;

        long totalMeetingsCount = meetingRepository.count();
        long upcomingMeetingsCount = meetingRepository.findByStartDateTimeGreaterThanEqual(LocalDateTime.now()).size();

        long openComplaintsCount = complaintRepository.findByStatus(ComplaintStatus.OPEN).size();
        long inProgressComplaintsCount = complaintRepository.findByStatus(ComplaintStatus.IN_PROGRESS).size();
        long resolvedComplaintsCount = complaintRepository.findByStatus(ComplaintStatus.RESOLVED).size();

        long activeAnnouncementsCount = announcementRepository.findByActive(true).size();

        Map<String, Long> tasksByPriority = new LinkedHashMap<>();
        for (TaskPriority p : TaskPriority.values()) {
            tasksByPriority.put(p.name(), allTasks.stream().filter(t -> t.getPriority() == p).count());
        }

        List<Complaint> allComplaints = complaintRepository.findAll();
        Map<String, Long> complaintsByCategory = new LinkedHashMap<>();
        for (ComplaintCategory c : ComplaintCategory.values()) {
            complaintsByCategory.put(c.name(), allComplaints.stream().filter(item -> item.getCategory() == c).count());
        }

        Map<String, Long> employeesByDepartment = new LinkedHashMap<>();
        for (Department dept : departmentRepository.findByActive(true)) {
            long count = employeeRepository.findByDepartmentIdAndStatus(dept.getId(), EmployeeStatus.ACTIVE).size();
            employeesByDepartment.put(dept.getName(), count);
        }

        return AdminDashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .totalDepartments(totalDepartments)
                .todayAttendanceCount(todayAttendanceCount)
                .todayPresentCount(todayPresentCount)
                .todayLateCount(todayLateCount)
                .todayAttendanceRate(todayAttendanceRate)
                .pendingLeavesCount(pendingLeavesCount)
                .approvedLeavesCount(approvedLeavesCount)
                .rejectedLeavesCount(rejectedLeavesCount)
                .totalTasksCount(totalTasksCount)
                .completedTasksCount(completedTasksCount)
                .pendingTasksCount(pendingTasksCount)
                .overdueTasksCount(overdueTasksCount)
                .taskCompletionRate(taskCompletionRate)
                .totalMeetingsCount(totalMeetingsCount)
                .upcomingMeetingsCount(upcomingMeetingsCount)
                .openComplaintsCount(openComplaintsCount)
                .inProgressComplaintsCount(inProgressComplaintsCount)
                .resolvedComplaintsCount(resolvedComplaintsCount)
                .activeAnnouncementsCount(activeAnnouncementsCount)
                .tasksByPriority(tasksByPriority)
                .complaintsByCategory(complaintsByCategory)
                .employeesByDepartment(employeesByDepartment)
                .build();
    }

    @Override
    public ManagerDashboardResponse getManagerDashboard(UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.MANAGER && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only managers and administrators can access the manager dashboard");
        }

        Optional<Employee> managerOpt = employeeRepository.findByUserId(currentUser.getId());
        Employee manager = managerOpt.orElse(null);

        Long deptId = (manager != null && manager.getDepartment() != null) ? manager.getDepartment().getId() : null;
        String deptName = (manager != null && manager.getDepartment() != null) ? manager.getDepartment().getName() : "All Departments";

        List<Employee> teamEmployees = deptId != null ?
                employeeRepository.findByDepartmentId(deptId) :
                (manager != null ? employeeRepository.findByManagerId(manager.getId()) : employeeRepository.findByStatus(EmployeeStatus.ACTIVE));

        long teamSize = teamEmployees.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).count();

        long todayTeamPresentCount = 0;
        if (deptId != null) {
            todayTeamPresentCount = attendanceRepository.findByEmployeeDepartmentIdAndAttendanceDate(deptId, LocalDate.now())
                    .stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        } else if (!teamEmployees.isEmpty()) {
            List<Long> ids = teamEmployees.stream().map(Employee::getId).toList();
            todayTeamPresentCount = attendanceRepository.findByAttendanceDate(LocalDate.now()).stream()
                    .filter(a -> ids.contains(a.getEmployee().getId()) && a.getStatus() == AttendanceStatus.PRESENT).count();
        }
        double todayTeamAttendanceRate = teamSize > 0 ?
                Math.round((double) todayTeamPresentCount / teamSize * 1000.0) / 10.0 : 0.0;

        List<Task> teamTasks = deptId != null ?
                taskRepository.findByAssignedToDepartmentId(deptId) :
                taskRepository.findAll();

        long teamTotalTasks = teamTasks.size();
        long teamCompletedTasks = teamTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long teamPendingTasks = teamTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long teamOverdueTasks = teamTasks.stream().filter(t ->
                t.getDeadline() != null &&
                        t.getDeadline().isBefore(LocalDateTime.now()) &&
                        t.getStatus() != TaskStatus.COMPLETED &&
                        t.getStatus() != TaskStatus.CANCELLED
        ).count();
        double teamTaskCompletionRate = teamTotalTasks > 0 ?
                Math.round((double) teamCompletedTasks / teamTotalTasks * 1000.0) / 10.0 : 0.0;

        Map<String, Long> teamTasksByStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            teamTasksByStatus.put(s.name(), teamTasks.stream().filter(t -> t.getStatus() == s).count());
        }

        long teamPendingLeavesCount = deptId != null ?
                leaveRequestRepository.findByEmployeeDepartmentIdAndStatus(deptId, LeaveStatus.PENDING).size() :
                leaveRequestRepository.findByStatus(LeaveStatus.PENDING).size();

        long teamOpenComplaintsCount = deptId != null ?
                complaintRepository.findByEmployeeDepartmentId(deptId).stream()
                        .filter(c -> c.getStatus() == ComplaintStatus.OPEN || c.getStatus() == ComplaintStatus.IN_PROGRESS).count() :
                complaintRepository.findByStatus(ComplaintStatus.OPEN).size();

        long managerUpcomingMeetingsCount = manager != null ?
                meetingParticipantRepository.findByEmployeeIdAndMeetingStartDateTimeGreaterThanEqual(manager.getId(), LocalDateTime.now()).size() :
                meetingRepository.findByStartDateTimeGreaterThanEqual(LocalDateTime.now()).size();

        return ManagerDashboardResponse.builder()
                .managerId(manager != null ? manager.getId() : currentUser.getId())
                .managerName(manager != null ? (manager.getFirstName() + " " + manager.getLastName()) : currentUser.getUsername())
                .departmentName(deptName)
                .teamSize(teamSize)
                .todayTeamPresentCount(todayTeamPresentCount)
                .todayTeamAttendanceRate(todayTeamAttendanceRate)
                .teamTotalTasks(teamTotalTasks)
                .teamCompletedTasks(teamCompletedTasks)
                .teamPendingTasks(teamPendingTasks)
                .teamOverdueTasks(teamOverdueTasks)
                .teamTaskCompletionRate(teamTaskCompletionRate)
                .teamPendingLeavesCount(teamPendingLeavesCount)
                .teamOpenComplaintsCount(teamOpenComplaintsCount)
                .managerUpcomingMeetingsCount(managerUpcomingMeetingsCount)
                .teamTasksByStatus(teamTasksByStatus)
                .build();
    }

    @Override
    public EmployeeDashboardResponse getEmployeeDashboard(UserPrincipal currentUser) {
        Employee employee = employeeRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user: " + currentUser.getUsername()));

        Long empId = employee.getId();

        List<Attendance> attendances = attendanceRepository.findByEmployeeId(empId);
        long totalAttendanceDays = attendances.size();
        long presentDays = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        double attendancePercentage = totalAttendanceDays > 0 ?
                Math.round((double) presentDays / totalAttendanceDays * 1000.0) / 10.0 : 0.0;

        List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeId(empId);
        Map<String, Integer> leaveBalances = new LinkedHashMap<>();
        for (LeaveBalance b : balances) {
            leaveBalances.put(b.getLeaveType().name(), b.getRemainingLeaves());
        }
        long pendingLeavesCount = leaveRequestRepository.findByEmployeeIdAndStatus(empId, LeaveStatus.PENDING).size();

        List<Task> tasks = taskRepository.findByAssignedToId(empId);
        long assignedTasksCount = tasks.size();
        long completedTasksCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long pendingTasksCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long overdueTasksCount = tasks.stream().filter(t ->
                t.getDeadline() != null &&
                        t.getDeadline().isBefore(LocalDateTime.now()) &&
                        t.getStatus() != TaskStatus.COMPLETED &&
                        t.getStatus() != TaskStatus.CANCELLED
        ).count();

        List<TaskResponse> recentTasks = tasks.stream()
                .sorted(Comparator.comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(this::mapToTaskResponse)
                .toList();

        List<MeetingParticipant> participants = meetingParticipantRepository
                .findByEmployeeIdAndMeetingStartDateTimeGreaterThanEqual(empId, LocalDateTime.now());
        long upcomingMeetingsCount = participants.size();
        List<MeetingResponse> upcomingMeetings = participants.stream()
                .map(MeetingParticipant::getMeeting)
                .distinct()
                .sorted(Comparator.comparing(Meeting::getStartDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .map(this::mapToMeetingResponse)
                .toList();

        long myOpenComplaintsCount = complaintRepository.findByEmployeeId(empId).stream()
                .filter(c -> c.getStatus() == ComplaintStatus.OPEN || c.getStatus() == ComplaintStatus.IN_PROGRESS).count();

        long activeAnnouncementsCount = announcementRepository.findByActive(true).size();

        return EmployeeDashboardResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .employeeCode(employee.getEmployeeCode())
                .departmentName(employee.getDepartment() != null ? employee.getDepartment().getName() : "Unassigned")
                .totalAttendanceDays(totalAttendanceDays)
                .presentDays(presentDays)
                .attendancePercentage(attendancePercentage)
                .leaveBalances(leaveBalances)
                .pendingLeavesCount(pendingLeavesCount)
                .assignedTasksCount(assignedTasksCount)
                .completedTasksCount(completedTasksCount)
                .pendingTasksCount(pendingTasksCount)
                .overdueTasksCount(overdueTasksCount)
                .upcomingMeetingsCount(upcomingMeetingsCount)
                .myOpenComplaintsCount(myOpenComplaintsCount)
                .activeAnnouncementsCount(activeAnnouncementsCount)
                .recentTasks(recentTasks)
                .upcomingMeetings(upcomingMeetings)
                .build();
    }

    private ScopeFilter resolveScope(Long departmentId, Long employeeId, UserPrincipal currentUser, String moduleName) {
        if (currentUser.getRole() == Role.ADMIN) {
            return new ScopeFilter(departmentId, employeeId);
        }

        Optional<Employee> empOpt = employeeRepository.findByUserId(currentUser.getId());
        if (empOpt.isEmpty()) {
            throw new AccessDeniedException("User has no associated employee profile to access " + moduleName + " analytics");
        }
        Employee employee = empOpt.get();

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (departmentId != null) {
                throw new AccessDeniedException("Employees cannot view department-wide " + moduleName + " analytics");
            }
            if (employeeId != null && !employeeId.equals(employee.getId())) {
                throw new AccessDeniedException("Employees can only view their own " + moduleName + " analytics");
            }
            return new ScopeFilter(null, employee.getId());
        }

        if (currentUser.getRole() == Role.MANAGER) {
            Long managerDeptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;

            if (departmentId != null && managerDeptId != null && !departmentId.equals(managerDeptId)) {
                throw new AccessDeniedException("Managers can only view " + moduleName + " analytics for their own department");
            }

            if (employeeId != null) {
                Employee target = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
                boolean isReport = target.getManager() != null && target.getManager().getId().equals(employee.getId());
                boolean isSameDept = target.getDepartment() != null && managerDeptId != null && target.getDepartment().getId().equals(managerDeptId);
                boolean isSelf = target.getId().equals(employee.getId());
                if (!isReport && !isSameDept && !isSelf) {
                    throw new AccessDeniedException("Managers can only view " + moduleName + " analytics for team members in their department");
                }
                return new ScopeFilter(managerDeptId, employeeId);
            }

            return new ScopeFilter(managerDeptId, null);
        }

        throw new AccessDeniedException("Access denied to " + moduleName + " analytics");
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null ? (task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName()) : null)
                .assignedById(task.getAssignedBy() != null ? task.getAssignedBy().getId() : null)
                .assignedByName(task.getAssignedBy() != null ? (task.getAssignedBy().getFirstName() + " " + task.getAssignedBy().getLastName()) : null)
                .priority(task.getPriority())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private MeetingResponse mapToMeetingResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .startDateTime(meeting.getStartDateTime())
                .endDateTime(meeting.getEndDateTime())
                .agenda(meeting.getAgenda())
                .status(meeting.getStatus())
                .organizerId(meeting.getOrganizer() != null ? meeting.getOrganizer().getId() : null)
                .organizerName(meeting.getOrganizer() != null ? (meeting.getOrganizer().getFirstName() + " " + meeting.getOrganizer().getLastName()) : null)
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }

    private record ScopeFilter(Long departmentId, Long employeeId) {}
}

package com.smartoffice.service.impl;

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
import com.smartoffice.entity.Task;
import com.smartoffice.entity.enums.AttendanceStatus;
import com.smartoffice.entity.enums.ComplaintStatus;
import com.smartoffice.entity.enums.LeaveStatus;
import com.smartoffice.entity.enums.Role;
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
import com.smartoffice.service.AiService;
import com.smartoffice.service.ai.AiProvider;
import com.smartoffice.service.ai.FallbackAiProvider;
import com.smartoffice.service.ai.OllamaAiProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AiServiceImpl implements AiService {

    private final OllamaAiProvider ollamaProvider;
    private final FallbackAiProvider fallbackProvider;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final ComplaintRepository complaintRepository;
    private final AnnouncementRepository announcementRepository;

    public AiServiceImpl(
            OllamaAiProvider ollamaProvider,
            FallbackAiProvider fallbackProvider,
            DepartmentRepository departmentRepository,
            EmployeeRepository employeeRepository,
            TaskRepository taskRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            ComplaintRepository complaintRepository,
            AnnouncementRepository announcementRepository) {
        this.ollamaProvider = ollamaProvider;
        this.fallbackProvider = fallbackProvider;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.complaintRepository = complaintRepository;
        this.announcementRepository = announcementRepository;
    }

    private AiProvider getActiveProvider() {
        return ollamaProvider.isAvailable() ? ollamaProvider : fallbackProvider;
    }

    @Override
    public ComplaintClassificationResponse classifyComplaint(ComplaintClassificationRequest request) {
        List<Department> departments = departmentRepository.findByActive(true);
        return getActiveProvider().classifyComplaint(request.getTitle(), request.getDescription(), departments);
    }

    @Override
    public TaskSubtaskSuggestionResponse suggestSubtasks(TaskSubtaskSuggestionRequest request) {
        return getActiveProvider().suggestSubtasks(
                request.getTaskTitle(),
                request.getTaskDescription(),
                request.getPriority(),
                request.getDeadline()
        );
    }

    @Override
    public EmployeePerformanceSummaryResponse getEmployeePerformanceSummary(Long employeeId, UserPrincipal currentUser) {
        Employee targetEmployee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        validateEmployeeAccess(targetEmployee, currentUser, "view performance summary of");

        List<Task> tasks = taskRepository.findByAssignedToId(employeeId);
        int assignedTasks = tasks.size();
        int completedTasks = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        int pendingTasks = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS).count();
        int overdueTasks = (int) tasks.stream().filter(t ->
                t.getDeadline() != null &&
                        t.getDeadline().isBefore(LocalDateTime.now()) &&
                        t.getStatus() != TaskStatus.COMPLETED &&
                        t.getStatus() != TaskStatus.CANCELLED
        ).count();
        double completionPercentage = assignedTasks > 0 ? Math.round((double) completedTasks / assignedTasks * 1000.0) / 10.0 : 0.0;

        List<Attendance> attendances = attendanceRepository.findByEmployeeId(employeeId);
        int totalAttendanceDays = attendances.size();
        int presentDays = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int lateDays = (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        double attendancePercentage = totalAttendanceDays > 0 ? Math.round((double) presentDays / totalAttendanceDays * 1000.0) / 10.0 : 0.0;

        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeId(employeeId);
        int totalLeaveRequests = leaveRequests.size();
        int approvedLeaves = (int) leaveRequests.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
        int pendingLeaves = (int) leaveRequests.stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
        int usedLeaveDays = leaveRequests.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED && l.getStartDate() != null && l.getEndDate() != null)
                .mapToInt(l -> (int) (ChronoUnit.DAYS.between(l.getStartDate(), l.getEndDate()) + 1))
                .sum();

        int scheduledMeetings = meetingParticipantRepository.findByEmployeeId(employeeId).size();

        List<Complaint> complaints = complaintRepository.findByEmployeeId(employeeId);
        int filedComplaints = complaints.size();
        int resolvedComplaints = (int) complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED || c.getStatus() == ComplaintStatus.CLOSED).count();

        List<String> observations = new ArrayList<>();
        observations.add("Task completion rate is " + completionPercentage + "% (" + completedTasks + "/" + assignedTasks + " completed).");
        if (overdueTasks > 0) {
            observations.add("There are " + overdueTasks + " overdue task(s) requiring attention.");
        } else {
            observations.add("No overdue tasks currently pending.");
        }
        observations.add("Attendance rate is " + attendancePercentage + "% across " + totalAttendanceDays + " recorded working days.");
        if (lateDays > 0) {
            observations.add("Recorded " + lateDays + " late check-in(s).");
        } else {
            observations.add("Consistent punctuality with zero late check-ins recorded.");
        }
        observations.add("Filed " + totalLeaveRequests + " leave request(s) (" + approvedLeaves + " approved, " + usedLeaveDays + " total used days).");
        observations.add("Participated in " + scheduledMeetings + " scheduled meeting(s).");

        return EmployeePerformanceSummaryResponse.builder()
                .employeeId(targetEmployee.getId())
                .employeeName(targetEmployee.getFirstName() + " " + targetEmployee.getLastName())
                .employeeCode(targetEmployee.getEmployeeCode())
                .departmentName(targetEmployee.getDepartment() != null ? targetEmployee.getDepartment().getName() : "Unassigned")
                .reportingPeriod("All Time")
                .assignedTasks(assignedTasks)
                .completedTasks(completedTasks)
                .pendingTasks(pendingTasks)
                .completionPercentage(completionPercentage)
                .overdueTasks(overdueTasks)
                .totalAttendanceDays(totalAttendanceDays)
                .presentDays(presentDays)
                .lateDays(lateDays)
                .attendancePercentage(attendancePercentage)
                .totalLeaveRequests(totalLeaveRequests)
                .approvedLeaves(approvedLeaves)
                .pendingLeaves(pendingLeaves)
                .usedLeaveDays(usedLeaveDays)
                .scheduledMeetingsCount(scheduledMeetings)
                .filedComplaintsCount(filedComplaints)
                .resolvedComplaintsCount(resolvedComplaints)
                .observations(observations)
                .disclaimer("This summary is informational only and does not make or replace automated employment or disciplinary decisions.")
                .build();
    }

    @Override
    public MeetingNotesSummaryResponse summarizeMeetingNotes(MeetingNotesSummaryRequest request, UserPrincipal currentUser) {
        Meeting meeting = meetingRepository.findById(request.getMeetingId())
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id: " + request.getMeetingId()));

        if (currentUser.getRole() != Role.ADMIN) {
            Optional<Employee> userEmployeeOpt = employeeRepository.findByUserId(currentUser.getId());
            if (userEmployeeOpt.isEmpty()) {
                throw new AccessDeniedException("User is not authorized to access notes for this meeting");
            }
            Employee userEmployee = userEmployeeOpt.get();
            boolean isOrganizer = meeting.getOrganizer() != null && meeting.getOrganizer().getId().equals(userEmployee.getId());
            boolean isParticipant = meetingParticipantRepository.existsByMeetingIdAndEmployeeId(meeting.getId(), userEmployee.getId());

            if (!isOrganizer && !isParticipant) {
                throw new AccessDeniedException("You are not authorized to view or summarize notes for this meeting");
            }
        }

        return getActiveProvider().summarizeMeeting(request.getMeetingId(), request.getNotes());
    }

    @Override
    public AssistantChatResponse chatAssistant(AssistantChatRequest request, UserPrincipal currentUser) {
        String msg = (request.getMessage() != null ? request.getMessage() : "").trim();
        String lower = msg.toLowerCase(Locale.ROOT);

        if (lower.contains("password") || lower.contains("secret") || lower.contains("jwt") ||
                lower.contains("token") || lower.contains("credential") || lower.contains("hash") ||
                lower.contains("api key")) {
            return AssistantChatResponse.builder()
                    .answer("I cannot assist with queries regarding system credentials, passwords, tokens, or security configuration.")
                    .mode("SECURITY_ENFORCED")
                    .data(null)
                    .build();
        }

        Optional<Employee> employeeOpt = employeeRepository.findByUserId(currentUser.getId());

        if (lower.contains("task") || lower.contains("todo") || lower.contains("pending")) {
            if (employeeOpt.isPresent()) {
                Long empId = employeeOpt.get().getId();
                List<Task> pending = taskRepository.findByAssignedToId(empId).stream()
                        .filter(t -> t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS)
                        .toList();
                String answer = "You have " + pending.size() + " active task(s): " +
                        (pending.isEmpty() ? "None pending!" : pending.stream().map(Task::getTitle).reduce((a, b) -> a + ", " + b).orElse(""));
                return AssistantChatResponse.builder()
                        .answer(answer)
                        .mode("DATABASE")
                        .data(pending.stream().map(t -> Map.of("id", t.getId(), "title", t.getTitle(), "status", t.getStatus().name())).toList())
                        .build();
            } else if (currentUser.getRole() == Role.ADMIN) {
                long count = taskRepository.count();
                return AssistantChatResponse.builder()
                        .answer("Total company tasks in system: " + count)
                        .mode("DATABASE")
                        .data(Map.of("totalTasks", count))
                        .build();
            }
        }

        if (lower.contains("leave") || lower.contains("vacation") || lower.contains("day off") || lower.contains("balance")) {
            if (employeeOpt.isPresent()) {
                Long empId = employeeOpt.get().getId();
                List<LeaveBalance> balances = leaveBalanceRepository.findByEmployeeId(empId);
                Map<String, Integer> balMap = new HashMap<>();
                balances.forEach(b -> balMap.put(b.getLeaveType().name(), b.getRemainingLeaves()));
                List<LeaveRequest> pending = leaveRequestRepository.findByEmployeeIdAndStatus(empId, LeaveStatus.PENDING);
                String answer = "Your leave balances: " + balMap + ". You have " + pending.size() + " pending leave request(s).";
                return AssistantChatResponse.builder()
                        .answer(answer)
                        .mode("DATABASE")
                        .data(Map.of("balances", balMap, "pendingCount", pending.size()))
                        .build();
            }
        }

        if (lower.contains("attendance") || lower.contains("present") || lower.contains("check in") || lower.contains("late")) {
            if (employeeOpt.isPresent()) {
                Long empId = employeeOpt.get().getId();
                List<Attendance> list = attendanceRepository.findByEmployeeId(empId);
                long present = list.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
                double rate = list.isEmpty() ? 0.0 : Math.round((double) present / list.size() * 1000.0) / 10.0;
                Optional<Attendance> today = attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now());
                String todayStatus = today.map(a -> a.getStatus().name()).orElse("NOT_CHECKED_IN");
                String answer = "Your overall attendance rate is " + rate + "% (" + present + "/" + list.size() + " days). Today's status: " + todayStatus + ".";
                return AssistantChatResponse.builder()
                        .answer(answer)
                        .mode("DATABASE")
                        .data(Map.of("totalRecords", list.size(), "present", present, "rate", rate, "todayStatus", todayStatus))
                        .build();
            }
        }

        if (lower.contains("meeting") || lower.contains("schedule") || lower.contains("calendar")) {
            if (employeeOpt.isPresent()) {
                Long empId = employeeOpt.get().getId();
                List<Meeting> upcoming = meetingParticipantRepository
                        .findByEmployeeIdAndMeetingStartDateTimeGreaterThanEqual(empId, LocalDateTime.now())
                        .stream().map(mp -> mp.getMeeting()).distinct().toList();
                String answer = "You have " + upcoming.size() + " upcoming meeting(s): " +
                        (upcoming.isEmpty() ? "None scheduled." : upcoming.stream().map(Meeting::getTitle).reduce((a, b) -> a + ", " + b).orElse(""));
                return AssistantChatResponse.builder()
                        .answer(answer)
                        .mode("DATABASE")
                        .data(upcoming.stream().map(m -> Map.of("id", m.getId(), "title", m.getTitle(), "start", m.getStartDateTime())).toList())
                        .build();
            }
        }

        if (lower.contains("complaint") || lower.contains("ticket") || lower.contains("issue")) {
            if (employeeOpt.isPresent()) {
                Long empId = employeeOpt.get().getId();
                List<Complaint> complaints = complaintRepository.findByEmployeeId(empId);
                long open = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.OPEN || c.getStatus() == ComplaintStatus.IN_PROGRESS).count();
                String answer = "You have filed " + complaints.size() + " complaint(s), with " + open + " currently open/in progress.";
                return AssistantChatResponse.builder()
                        .answer(answer)
                        .mode("DATABASE")
                        .data(Map.of("totalComplaints", complaints.size(), "openComplaints", open))
                        .build();
            }
        }

        if (lower.contains("announcement") || lower.contains("notice") || lower.contains("news")) {
            List<Announcement> active = announcementRepository.findByActive(true);
            String answer = "There are " + active.size() + " active company announcement(s): " +
                    (active.isEmpty() ? "No announcements currently active." : active.stream().map(Announcement::getTitle).reduce((a, b) -> a + "; " + b).orElse(""));
            return AssistantChatResponse.builder()
                    .answer(answer)
                    .mode("DATABASE")
                    .data(active.stream().map(a -> Map.of("id", a.getId(), "title", a.getTitle())).toList())
                    .build();
        }

        AiProvider provider = getActiveProvider();
        String context = "User: " + currentUser.getUsername() + ", Role: " + currentUser.getRole();
        String answer = provider.generateOfficeAssistantAnswer(msg, context);

        return AssistantChatResponse.builder()
                .answer(answer)
                .mode(provider.getProviderName())
                .data(null)
                .build();
    }

    private void validateEmployeeAccess(Employee targetEmployee, UserPrincipal currentUser, String action) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        Optional<Employee> currentEmpOpt = employeeRepository.findByUserId(currentUser.getId());
        if (currentEmpOpt.isEmpty()) {
            throw new AccessDeniedException("User has no associated employee profile to " + action + " employee records");
        }

        Employee currentEmployee = currentEmpOpt.get();

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (!currentEmployee.getId().equals(targetEmployee.getId())) {
                throw new AccessDeniedException("Employees can only " + action + " their own record");
            }
            return;
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (currentEmployee.getId().equals(targetEmployee.getId())) {
                return;
            }
            boolean isDirectReport = targetEmployee.getManager() != null && targetEmployee.getManager().getId().equals(currentEmployee.getId());
            boolean isSameDepartment = currentEmployee.getDepartment() != null &&
                    targetEmployee.getDepartment() != null &&
                    currentEmployee.getDepartment().getId().equals(targetEmployee.getDepartment().getId());

            if (!isDirectReport && !isSameDepartment) {
                throw new AccessDeniedException("Managers can only " + action + " employees within their authorized scope");
            }
        }
    }
}

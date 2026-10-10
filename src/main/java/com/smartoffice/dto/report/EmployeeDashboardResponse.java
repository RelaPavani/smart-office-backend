package com.smartoffice.dto.report;

import com.smartoffice.dto.meeting.MeetingResponse;
import com.smartoffice.dto.task.TaskResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDashboardResponse {

    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;

    // Attendance
    private long totalAttendanceDays;
    private long presentDays;
    private double attendancePercentage;

    // Leaves
    private Map<String, Integer> leaveBalances;
    private long pendingLeavesCount;

    // Tasks
    private long assignedTasksCount;
    private long completedTasksCount;
    private long pendingTasksCount;
    private long overdueTasksCount;

    // Meetings & Complaints
    private long upcomingMeetingsCount;
    private long myOpenComplaintsCount;
    private long activeAnnouncementsCount;

    // Recent items
    private List<TaskResponse> recentTasks;
    private List<MeetingResponse> upcomingMeetings;
}

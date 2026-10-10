package com.smartoffice.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {

    // Workforce
    private long totalEmployees;
    private long activeEmployees;
    private long totalDepartments;

    // Attendance
    private long todayAttendanceCount;
    private long todayPresentCount;
    private long todayLateCount;
    private double todayAttendanceRate;

    // Leaves
    private long pendingLeavesCount;
    private long approvedLeavesCount;
    private long rejectedLeavesCount;

    // Tasks
    private long totalTasksCount;
    private long completedTasksCount;
    private long pendingTasksCount;
    private long overdueTasksCount;
    private double taskCompletionRate;

    // Meetings
    private long totalMeetingsCount;
    private long upcomingMeetingsCount;

    // Complaints
    private long openComplaintsCount;
    private long inProgressComplaintsCount;
    private long resolvedComplaintsCount;

    // Announcements
    private long activeAnnouncementsCount;

    // Breakdowns
    private Map<String, Long> tasksByPriority;
    private Map<String, Long> complaintsByCategory;
    private Map<String, Long> employeesByDepartment;
}

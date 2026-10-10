package com.smartoffice.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeePerformanceSummaryResponse {

    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String departmentName;
    private String reportingPeriod;

    // Task Metrics
    private int assignedTasks;
    private int completedTasks;
    private int pendingTasks;
    private double completionPercentage;
    private int overdueTasks;

    // Attendance Metrics
    private int totalAttendanceDays;
    private int presentDays;
    private int lateDays;
    private double attendancePercentage;

    // Leave Metrics
    private int totalLeaveRequests;
    private int approvedLeaves;
    private int pendingLeaves;
    private int usedLeaveDays;

    // Meetings
    private int scheduledMeetingsCount;

    // Complaints
    private int filedComplaintsCount;
    private int resolvedComplaintsCount;

    // Observations & Disclaimer
    private List<String> observations;
    @Builder.Default
    private String disclaimer = "This summary is informational only and does not make or replace automated employment or disciplinary decisions.";
}

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
public class ManagerDashboardResponse {

    private Long managerId;
    private String managerName;
    private String departmentName;

    // Team stats
    private long teamSize;
    private long todayTeamPresentCount;
    private double todayTeamAttendanceRate;

    // Tasks
    private long teamTotalTasks;
    private long teamCompletedTasks;
    private long teamPendingTasks;
    private long teamOverdueTasks;
    private double teamTaskCompletionRate;

    // Leaves & Complaints
    private long teamPendingLeavesCount;
    private long teamOpenComplaintsCount;
    private long managerUpcomingMeetingsCount;

    // Breakdowns
    private Map<String, Long> teamTasksByStatus;
}

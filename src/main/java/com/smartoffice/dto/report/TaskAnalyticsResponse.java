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
public class TaskAnalyticsResponse {

    private long totalTasks;
    private long completedTasks;
    private long inProgressTasks;
    private long todoTasks;
    private long cancelledTasks;
    private long overdueTasks;
    private double completionRate;

    private Map<String, Long> tasksByPriority;
    private Map<String, Long> tasksByStatus;
}

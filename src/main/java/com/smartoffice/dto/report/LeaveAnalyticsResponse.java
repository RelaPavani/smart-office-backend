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
public class LeaveAnalyticsResponse {

    private long totalLeaveRequests;
    private long pendingLeaves;
    private long approvedLeaves;
    private long rejectedLeaves;
    private double approvalRate;

    private Map<String, Long> leavesByType;
    private Map<String, Long> leavesByStatus;
}

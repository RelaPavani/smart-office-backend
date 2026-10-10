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
public class AttendanceAnalyticsResponse {

    private long totalRecords;
    private long presentCount;
    private long lateCount;
    private long halfDayCount;
    private long absentCount;
    private double overallAttendanceRate;

    private Map<String, Long> attendanceByStatus;
}

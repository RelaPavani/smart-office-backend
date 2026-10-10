package com.smartoffice.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryResponse {

    private Long employeeId;
    private String employeeName;
    private Integer year;
    private Integer month;
    private int totalWorkingDays;
    private int presentDays;
    private int absentDays;
    private int lateDays;
    private int halfDays;
    private double attendancePercentage;
}

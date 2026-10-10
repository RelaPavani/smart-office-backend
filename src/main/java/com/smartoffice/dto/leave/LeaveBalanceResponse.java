package com.smartoffice.dto.leave;

import com.smartoffice.entity.enums.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer year;
    private LeaveType leaveType;
    private Integer totalLeaves;
    private Integer usedLeaves;
    private Integer remainingLeaves;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

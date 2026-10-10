package com.smartoffice.dto.leave;

import com.smartoffice.entity.enums.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class LeaveBalanceCreateRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Total leaves is required")
    @PositiveOrZero(message = "Total leaves must be zero or positive")
    private Integer totalLeaves;

    @PositiveOrZero(message = "Used leaves must be zero or positive")
    @Builder.Default
    private Integer usedLeaves = 0;

    @NotNull(message = "Remaining leaves is required")
    @PositiveOrZero(message = "Remaining leaves must be zero or positive")
    private Integer remainingLeaves;
}

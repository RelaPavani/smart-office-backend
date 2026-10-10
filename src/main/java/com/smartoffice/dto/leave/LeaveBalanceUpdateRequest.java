package com.smartoffice.dto.leave;

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
public class LeaveBalanceUpdateRequest {

    @PositiveOrZero(message = "Total leaves must be zero or positive")
    private Integer totalLeaves;

    @PositiveOrZero(message = "Used leaves must be zero or positive")
    private Integer usedLeaves;

    @PositiveOrZero(message = "Remaining leaves must be zero or positive")
    private Integer remainingLeaves;
}

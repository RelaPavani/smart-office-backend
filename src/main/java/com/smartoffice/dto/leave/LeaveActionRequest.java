package com.smartoffice.dto.leave;

import jakarta.validation.constraints.Size;
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
public class LeaveActionRequest {

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}

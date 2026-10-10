package com.smartoffice.dto.ai;

import jakarta.validation.constraints.NotBlank;
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
public class ComplaintClassificationRequest {

    @NotBlank(message = "Complaint title is required")
    private String title;

    @NotBlank(message = "Complaint description is required")
    private String description;
}

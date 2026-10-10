package com.smartoffice.dto.ai;

import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
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
public class ComplaintClassificationResponse {

    private ComplaintCategory suggestedCategory;
    private ComplaintPriority suggestedPriority;
    private String suggestedDepartment;
    private Long suggestedDepartmentId;
    private String explanation;
    private Double confidence;
    private String mode;
}

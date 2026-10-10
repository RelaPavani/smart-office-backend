package com.smartoffice.dto.complaint;

import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
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
public class ComplaintUpdateRequest {

    private ComplaintCategory category;

    private ComplaintPriority priority;

    private ComplaintStatus status;

    private Long assignedToId;

    private String resolution;
}

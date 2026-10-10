package com.smartoffice.dto.meeting;

import com.smartoffice.entity.enums.ParticipantStatus;
import jakarta.validation.constraints.NotNull;
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
public class MeetingParticipantCreateRequest {

    @NotNull(message = "Meeting ID is required")
    private Long meetingId;

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @Builder.Default
    private ParticipantStatus attendanceStatus = ParticipantStatus.INVITED;
}

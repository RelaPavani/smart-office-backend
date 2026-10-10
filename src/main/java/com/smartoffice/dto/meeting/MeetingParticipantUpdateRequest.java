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
public class MeetingParticipantUpdateRequest {

    @NotNull(message = "Attendance status is required")
    private ParticipantStatus attendanceStatus;
}

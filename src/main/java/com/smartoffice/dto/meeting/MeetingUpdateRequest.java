package com.smartoffice.dto.meeting;

import com.smartoffice.entity.enums.MeetingStatus;
import jakarta.validation.constraints.Size;
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
public class MeetingUpdateRequest {

    @Size(max = 150, message = "Meeting title cannot exceed 150 characters")
    private String title;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private String agenda;

    private MeetingStatus status;
}

package com.smartoffice.dto.meeting;

import com.smartoffice.entity.enums.MeetingStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class MeetingCreateRequest {

    @NotBlank(message = "Meeting title is required")
    @Size(max = 150, message = "Meeting title cannot exceed 150 characters")
    private String title;

    @NotNull(message = "Start date-time is required")
    @FutureOrPresent(message = "Meeting start time cannot be in the past")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date-time is required")
    private LocalDateTime endDateTime;

    private String agenda;

    @NotNull(message = "Organizer ID is required")
    private Long organizerId;

    @Builder.Default
    private MeetingStatus status = MeetingStatus.SCHEDULED;
}

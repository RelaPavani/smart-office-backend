package com.smartoffice.dto.ai;

import jakarta.validation.constraints.NotBlank;
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
public class MeetingNotesSummaryRequest {

    @NotNull(message = "Meeting ID is required")
    private Long meetingId;

    @NotBlank(message = "Meeting notes are required")
    private String notes;
}

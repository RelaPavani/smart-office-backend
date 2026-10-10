package com.smartoffice.dto.meeting;

import com.smartoffice.entity.enums.ParticipantStatus;
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
public class MeetingParticipantResponse {

    private Long id;
    private Long meetingId;
    private String meetingTitle;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private ParticipantStatus attendanceStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

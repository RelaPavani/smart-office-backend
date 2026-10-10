package com.smartoffice.dto.attendance;

import com.smartoffice.entity.enums.AttendanceStatus;
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
public class AttendanceUpdateRequest {

    private LocalDateTime checkIn;

    private LocalDateTime checkOut;

    private AttendanceStatus status;
}

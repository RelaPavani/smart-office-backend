package com.smartoffice.dto.announcement;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementUpdateRequest {

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String content;

    private LocalDate expiryDate;

    private Boolean active;
}

package com.smartoffice.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingNotesSummaryResponse {

    private Long meetingId;
    private String summary;
    private List<String> keyPoints;
    private List<String> actionItems;
    private String mode;
}

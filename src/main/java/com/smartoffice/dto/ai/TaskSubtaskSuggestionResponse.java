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
public class TaskSubtaskSuggestionResponse {

    private String taskTitle;
    private List<String> suggestedSubtasks;
    private String explanation;
    private String mode;
}

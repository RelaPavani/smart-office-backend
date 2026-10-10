package com.smartoffice.dto.ai;

import com.smartoffice.entity.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
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
public class TaskSubtaskSuggestionRequest {

    @NotBlank(message = "Task title is required")
    private String taskTitle;

    private String taskDescription;

    private TaskPriority priority;

    private LocalDateTime deadline;
}

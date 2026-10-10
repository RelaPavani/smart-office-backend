package com.smartoffice.dto.task;

import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
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
public class TaskUpdateRequest {

    @Size(max = 150, message = "Task title cannot exceed 150 characters")
    private String title;

    private String description;

    private Long assignedToId;

    private TaskPriority priority;

    private LocalDateTime deadline;

    private TaskStatus status;

    private LocalDateTime completedAt;
}

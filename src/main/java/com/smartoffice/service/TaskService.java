package com.smartoffice.service;

import com.smartoffice.dto.task.TaskCreateRequest;
import com.smartoffice.dto.task.TaskResponse;
import com.smartoffice.dto.task.TaskUpdateRequest;
import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
import com.smartoffice.security.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {

    TaskResponse createTask(TaskCreateRequest request, UserPrincipal currentUser);

    TaskResponse getTaskById(Long id, UserPrincipal currentUser);

    List<TaskResponse> listTasks(TaskStatus status, TaskPriority priority, Long assignedToId, String search, LocalDateTime deadlineBefore, UserPrincipal currentUser);

    List<TaskResponse> getMyTasks(TaskStatus status, TaskPriority priority, UserPrincipal currentUser);

    TaskResponse updateTask(Long id, TaskUpdateRequest request, UserPrincipal currentUser);

    TaskResponse updateTaskStatus(Long id, TaskStatus newStatus, UserPrincipal currentUser);

    void deleteTask(Long id, UserPrincipal currentUser);
}

package com.smartoffice.service.impl;

import com.smartoffice.dto.task.TaskCreateRequest;
import com.smartoffice.dto.task.TaskResponse;
import com.smartoffice.dto.task.TaskUpdateRequest;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.Task;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.TaskRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.TaskService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;

    public TaskServiceImpl(TaskRepository taskRepository, EmployeeRepository employeeRepository) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public TaskResponse createTask(TaskCreateRequest request, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot create or assign tasks");
        }

        if (request.getAssignedToId() == null) {
            throw new BadRequestException("Assigned employee ID is required");
        }

        Employee assignedTo = employeeRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getAssignedToId()));

        if (assignedTo.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Cannot assign task to an inactive employee");
        }

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        if (currentUser.getRole() == Role.MANAGER) {
            if (currentEmp == null) {
                throw new AccessDeniedException("Manager employee profile not found");
            }
            if (!isWithinManagerScope(currentEmp, assignedTo)) {
                throw new AccessDeniedException("Access denied: Managers can only assign tasks to team members within their department or reports");
            }
        }

        Employee assignedBy = null;
        if (currentUser.getRole() == Role.MANAGER) {
            assignedBy = currentEmp;
        } else if (currentUser.getRole() == Role.ADMIN) {
            if (request.getAssignedById() != null) {
                assignedBy = employeeRepository.findById(request.getAssignedById())
                        .orElse(currentEmp);
            } else {
                assignedBy = currentEmp;
            }
        }

        TaskStatus initialStatus = request.getStatus() != null ? request.getStatus() : TaskStatus.TODO;
        LocalDateTime completedAt = (initialStatus == TaskStatus.COMPLETED) ? LocalDateTime.now() : null;

        Task task = Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .assignedTo(assignedTo)
                .assignedBy(assignedBy)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .deadline(request.getDeadline())
                .status(initialStatus)
                .completedAt(completedAt)
                .build();

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, UserPrincipal currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        validateTaskReadAccess(task, currentUser);

        return mapToResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(TaskStatus status, TaskPriority priority, Long assignedToId, String search, LocalDateTime deadlineBefore, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        List<Task> allTasks = taskRepository.findAll();

        return allTasks.stream()
                .filter(t -> hasTaskReadPermission(t, currentUser, currentEmp))
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> assignedToId == null || (t.getAssignedTo() != null && t.getAssignedTo().getId().equals(assignedToId)))
                .filter(t -> deadlineBefore == null || (t.getDeadline() != null && !t.getDeadline().isAfter(deadlineBefore)))
                .filter(t -> {
                    if (!StringUtils.hasText(search)) {
                        return true;
                    }
                    String term = search.trim().toLowerCase();
                    boolean matchTitle = t.getTitle() != null && t.getTitle().toLowerCase().contains(term);
                    boolean matchDesc = t.getDescription() != null && t.getDescription().toLowerCase().contains(term);
                    return matchTitle || matchDesc;
                })
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(TaskStatus status, TaskPriority priority, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployee(currentUser);

        List<Task> myTasks;
        if (status != null) {
            myTasks = taskRepository.findByAssignedToIdAndStatus(currentEmp.getId(), status);
        } else if (priority != null) {
            myTasks = taskRepository.findByAssignedToIdAndPriority(currentEmp.getId(), priority);
        } else {
            myTasks = taskRepository.findByAssignedToId(currentEmp.getId());
        }

        return myTasks.stream()
                .filter(t -> priority == null || t.getPriority() == priority)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, TaskUpdateRequest request, UserPrincipal currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot modify task details. Use status update endpoint instead.");
        }

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
        if (currentUser.getRole() == Role.MANAGER) {
            validateManagerTaskModifyAccess(task, currentEmp);
        }

        if (StringUtils.hasText(request.getTitle())) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getAssignedToId() != null) {
            Employee newAssignedTo = employeeRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getAssignedToId()));
            if (newAssignedTo.getStatus() != EmployeeStatus.ACTIVE) {
                throw new BadRequestException("Cannot assign task to an inactive employee");
            }
            if (currentUser.getRole() == Role.MANAGER && !isWithinManagerScope(currentEmp, newAssignedTo)) {
                throw new AccessDeniedException("Access denied: Cannot reassign task to employee outside your team scope");
            }
            task.setAssignedTo(newAssignedTo);
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDeadline() != null) {
            task.setDeadline(request.getDeadline());
        }
        if (request.getStatus() != null) {
            applyStatusTransition(task, request.getStatus());
        }

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long id, TaskStatus newStatus, UserPrincipal currentUser) {
        if (newStatus == null) {
            throw new BadRequestException("New task status is required");
        }

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (currentEmp == null || task.getAssignedTo() == null || !task.getAssignedTo().getId().equals(currentEmp.getId())) {
                throw new AccessDeniedException("Access denied: You can only update the status of tasks assigned to you");
            }
            if (newStatus == TaskStatus.CANCELLED) {
                throw new AccessDeniedException("Access denied: Employees cannot cancel tasks");
            }
            if (task.getStatus() == TaskStatus.CANCELLED) {
                throw new BadRequestException("Cannot change status of a cancelled task");
            }
        } else if (currentUser.getRole() == Role.MANAGER) {
            validateManagerTaskModifyAccess(task, currentEmp);
        }

        applyStatusTransition(task, newStatus);

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTask(Long id, UserPrincipal currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot delete tasks");
        }

        if (currentUser.getRole() == Role.MANAGER) {
            Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
            validateManagerTaskModifyAccess(task, currentEmp);
        }

        taskRepository.delete(task);
    }

    private void applyStatusTransition(Task task, TaskStatus newStatus) {
        if (task.getStatus() == newStatus) {
            return;
        }

        if (newStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(null);
        }

        task.setStatus(newStatus);
    }

    private void validateTaskReadAccess(Task task, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
        if (!hasTaskReadPermission(task, currentUser, currentEmp)) {
            if (currentUser.getRole() == Role.EMPLOYEE) {
                throw new AccessDeniedException("Access denied: You can only view your own assigned tasks");
            } else {
                throw new AccessDeniedException("Access denied: You do not have permission to view this task");
            }
        }
    }

    private boolean hasTaskReadPermission(Task task, UserPrincipal currentUser, Employee currentEmp) {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            return currentEmp != null && task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentEmp.getId());
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (currentEmp == null) {
                return false;
            }
            boolean isAssignee = task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentEmp.getId());
            boolean isAssigner = task.getAssignedBy() != null && task.getAssignedBy().getId().equals(currentEmp.getId());
            boolean inScope = task.getAssignedTo() != null && isWithinManagerScope(currentEmp, task.getAssignedTo());
            return isAssignee || isAssigner || inScope;
        }

        return false;
    }

    private void validateManagerTaskModifyAccess(Task task, Employee currentEmp) {
        if (currentEmp == null) {
            throw new AccessDeniedException("Manager employee record not found");
        }
        boolean isAssigner = task.getAssignedBy() != null && task.getAssignedBy().getId().equals(currentEmp.getId());
        boolean isAssignee = task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentEmp.getId());
        boolean inScope = task.getAssignedTo() != null && isWithinManagerScope(currentEmp, task.getAssignedTo());

        if (!isAssigner && !isAssignee && !inScope) {
            throw new AccessDeniedException("Access denied: You can only modify tasks within your team scope");
        }
    }

    private boolean isWithinManagerScope(Employee managerEmp, Employee targetEmp) {
        if (managerEmp.getId().equals(targetEmp.getId())) {
            return true;
        }
        boolean isDirectReport = targetEmp.getManager() != null && targetEmp.getManager().getId().equals(managerEmp.getId());
        boolean isSameDept = targetEmp.getDepartment() != null && managerEmp.getDepartment() != null
                && targetEmp.getDepartment().getId().equals(managerEmp.getDepartment().getId());
        return isDirectReport || isSameDept;
    }

    private Employee resolveCurrentEmployee(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile not found for user: " + currentUser.getUsername()));
    }

    private Employee resolveCurrentEmployeeOrNull(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElse(null);
    }

    private TaskResponse mapToResponse(Task t) {
        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .assignedToId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                .assignedToName(t.getAssignedTo() != null ? t.getAssignedTo().getFirstName() + " " + t.getAssignedTo().getLastName() : null)
                .assignedById(t.getAssignedBy() != null ? t.getAssignedBy().getId() : null)
                .assignedByName(t.getAssignedBy() != null ? t.getAssignedBy().getFirstName() + " " + t.getAssignedBy().getLastName() : null)
                .priority(t.getPriority())
                .deadline(t.getDeadline())
                .status(t.getStatus())
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}

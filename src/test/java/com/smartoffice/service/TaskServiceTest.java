package com.smartoffice.service;

import com.smartoffice.dto.task.TaskCreateRequest;
import com.smartoffice.dto.task.TaskResponse;
import com.smartoffice.dto.task.TaskUpdateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.Task;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.TaskRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal otherEmpPrincipal;

    private Department deptEngineering;
    private Department deptSales;
    private Employee managerEmp;
    private Employee teamMemberEmp;
    private Employee otherDeptEmp;
    private Task testTask;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, "admin", "admin@smartoffice.com", "pass", Role.ADMIN, true, Collections.emptyList());
        managerPrincipal = new UserPrincipal(2L, "manager", "manager@smartoffice.com", "pass", Role.MANAGER, true, Collections.emptyList());
        employeePrincipal = new UserPrincipal(3L, "emp1", "emp1@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());
        otherEmpPrincipal = new UserPrincipal(4L, "emp2", "emp2@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());

        deptEngineering = Department.builder().id(10L).name("Engineering").active(true).build();
        deptSales = Department.builder().id(20L).name("Sales").active(true).build();

        managerEmp = Employee.builder()
                .id(100L)
                .employeeCode("MGR001")
                .firstName("Alice")
                .lastName("Manager")
                .email("manager@smartoffice.com")
                .department(deptEngineering)
                .status(EmployeeStatus.ACTIVE)
                .build();

        teamMemberEmp = Employee.builder()
                .id(101L)
                .employeeCode("EMP001")
                .firstName("Bob")
                .lastName("Dev")
                .email("emp1@smartoffice.com")
                .department(deptEngineering)
                .manager(managerEmp)
                .status(EmployeeStatus.ACTIVE)
                .build();

        otherDeptEmp = Employee.builder()
                .id(102L)
                .employeeCode("EMP002")
                .firstName("Charlie")
                .lastName("Sales")
                .email("emp2@smartoffice.com")
                .department(deptSales)
                .status(EmployeeStatus.ACTIVE)
                .build();

        testTask = Task.builder()
                .id(500L)
                .title("Complete Feature X")
                .description("Detailed description")
                .assignedTo(teamMemberEmp)
                .assignedBy(managerEmp)
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .deadline(LocalDateTime.now().plusDays(3))
                .build();
    }

    @Test
    @DisplayName("Admin creates task successfully")
    void testAdminCreateTask() {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Database Migration")
                .description("Run migrations")
                .assignedToId(101L)
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .build();

        when(employeeRepository.findById(101L)).thenReturn(Optional.of(teamMemberEmp));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(600L);
            return t;
        });

        TaskResponse response = taskService.createTask(request, adminPrincipal);

        assertNotNull(response);
        assertEquals(600L, response.getId());
        assertEquals("Database Migration", response.getTitle());
        assertEquals(101L, response.getAssignedToId());
    }

    @Test
    @DisplayName("Manager creates task for team member in scope")
    void testManagerCreateTaskInScope() {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Fix Security Bug")
                .assignedToId(101L)
                .priority(TaskPriority.HIGH)
                .build();

        when(employeeRepository.findById(101L)).thenReturn(Optional.of(teamMemberEmp));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(managerEmp));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.createTask(request, managerPrincipal);

        assertNotNull(response);
        assertEquals("Fix Security Bug", response.getTitle());
    }

    @Test
    @DisplayName("Manager cannot assign task to employee outside scope")
    void testManagerCreateTaskOutsideScopeThrows() {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Cross-dept task")
                .assignedToId(102L)
                .priority(TaskPriority.LOW)
                .build();

        when(employeeRepository.findById(102L)).thenReturn(Optional.of(otherDeptEmp));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(managerEmp));

        assertThrows(AccessDeniedException.class, () -> taskService.createTask(request, managerPrincipal));
    }

    @Test
    @DisplayName("Employee cannot create tasks")
    void testEmployeeCreateTaskThrowsAccessDenied() {
        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Unauthorized task")
                .assignedToId(101L)
                .priority(TaskPriority.LOW)
                .build();

        assertThrows(AccessDeniedException.class, () -> taskService.createTask(request, employeePrincipal));
    }

    @Test
    @DisplayName("Employee can read own assigned task")
    void testEmployeeCanReadOwnTask() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(testTask));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(teamMemberEmp));

        TaskResponse response = taskService.getTaskById(500L, employeePrincipal);

        assertNotNull(response);
        assertEquals(500L, response.getId());
    }

    @Test
    @DisplayName("Employee cannot read another employee's task")
    void testEmployeeCannotReadOtherEmployeeTask() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(testTask));
        when(employeeRepository.findByUserId(4L)).thenReturn(Optional.of(otherDeptEmp));

        assertThrows(AccessDeniedException.class, () -> taskService.getTaskById(500L, otherEmpPrincipal));
    }

    @Test
    @DisplayName("Employee updates own task status to COMPLETED sets completedAt")
    void testEmployeeUpdatesStatusToCompleted() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(testTask));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(teamMemberEmp));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(500L, TaskStatus.COMPLETED, employeePrincipal);

        assertEquals(TaskStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getCompletedAt());
    }

    @Test
    @DisplayName("Moving status away from COMPLETED clears completedAt")
    void testRevertingCompletedStatusClearsCompletedAt() {
        testTask.setStatus(TaskStatus.COMPLETED);
        testTask.setCompletedAt(LocalDateTime.now().minusHours(1));

        when(taskRepository.findById(500L)).thenReturn(Optional.of(testTask));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(teamMemberEmp));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(500L, TaskStatus.IN_PROGRESS, employeePrincipal);

        assertEquals(TaskStatus.IN_PROGRESS, response.getStatus());
        assertNull(response.getCompletedAt());
    }

    @Test
    @DisplayName("Employee cannot cancel tasks")
    void testEmployeeCannotCancelTask() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(testTask));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(teamMemberEmp));

        assertThrows(AccessDeniedException.class, () -> taskService.updateTaskStatus(500L, TaskStatus.CANCELLED, employeePrincipal));
    }

    @Test
    @DisplayName("Employee cannot delete tasks")
    void testEmployeeCannotDeleteTask() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(testTask));

        assertThrows(AccessDeniedException.class, () -> taskService.deleteTask(500L, employeePrincipal));
    }

    @Test
    @DisplayName("Admin deletes task successfully")
    void testAdminDeleteTask() {
        when(taskRepository.findById(500L)).thenReturn(Optional.of(testTask));

        taskService.deleteTask(500L, adminPrincipal);

        verify(taskRepository).delete(testTask);
    }
}

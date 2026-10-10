package com.smartoffice.repository;

import com.smartoffice.entity.Task;
import com.smartoffice.entity.enums.TaskPriority;
import com.smartoffice.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignedToId(Long employeeId);

    List<Task> findByAssignedToIdAndStatus(Long employeeId, TaskStatus status);

    List<Task> findByAssignedById(Long employeeId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByPriority(TaskPriority priority);

    List<Task> findByAssignedToIdAndPriority(Long employeeId, TaskPriority priority);

    List<Task> findByAssignedToDepartmentId(Long departmentId);
}

package com.smartoffice.dto.employee;

import com.smartoffice.entity.enums.EmployeeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String designation;
    private LocalDate joiningDate;
    private EmployeeStatus status;
    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerName;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.smartoffice.service;

import com.smartoffice.dto.employee.EmployeeCreateRequest;
import com.smartoffice.dto.employee.EmployeePersonalUpdateRequest;
import com.smartoffice.dto.employee.EmployeeResponse;
import com.smartoffice.dto.employee.EmployeeUpdateRequest;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.security.UserPrincipal;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    EmployeeResponse getEmployeeById(Long id, UserPrincipal currentUser);

    EmployeeResponse getCurrentEmployee(UserPrincipal currentUser);

    EmployeeResponse updatePersonalDetails(EmployeePersonalUpdateRequest request, UserPrincipal currentUser);

    List<EmployeeResponse> getAllEmployees(Long departmentId, EmployeeStatus status, String search, UserPrincipal currentUser);

    List<EmployeeResponse> getEmployeesByDepartment(Long departmentId, UserPrincipal currentUser);

    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);

    void deactivateOrDeleteEmployee(Long id);
}

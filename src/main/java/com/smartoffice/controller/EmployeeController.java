package com.smartoffice.controller;

import com.smartoffice.dto.employee.EmployeeCreateRequest;
import com.smartoffice.dto.employee.EmployeePersonalUpdateRequest;
import com.smartoffice.dto.employee.EmployeeResponse;
import com.smartoffice.dto.employee.EmployeeUpdateRequest;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return new ResponseEntity<>(ApiResponse.success("Employee created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<EmployeeResponse> employees = employeeService.getAllEmployees(departmentId, status, search, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved successfully", employees));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getCurrentEmployee(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeResponse response = employeeService.getCurrentEmployee(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee profile retrieved successfully", response));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateOwnProfile(
            @Valid @RequestBody EmployeePersonalUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeResponse response = employeeService.updatePersonalDetails(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesByDepartment(
            @PathVariable Long departmentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<EmployeeResponse> employees = employeeService.getEmployeesByDepartment(departmentId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Department employees retrieved successfully", employees));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeResponse response = employeeService.getEmployeeById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        EmployeeResponse response = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateOrDeleteEmployee(@PathVariable Long id) {
        employeeService.deactivateOrDeleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success("Employee deactivated successfully", null));
    }
}

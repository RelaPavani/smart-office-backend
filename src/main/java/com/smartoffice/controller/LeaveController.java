package com.smartoffice.controller;

import com.smartoffice.dto.leave.LeaveActionRequest;
import com.smartoffice.dto.leave.LeaveBalanceCreateRequest;
import com.smartoffice.dto.leave.LeaveBalanceResponse;
import com.smartoffice.dto.leave.LeaveBalanceUpdateRequest;
import com.smartoffice.dto.leave.LeaveRequestCreateRequest;
import com.smartoffice.dto.leave.LeaveRequestResponse;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> applyForLeave(
            @Valid @RequestBody LeaveRequestCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        LeaveRequestResponse response = leaveService.applyForLeave(request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Leave request submitted successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getMyLeaves(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<LeaveRequestResponse> list = leaveService.getMyLeaves(currentUser);
        return ResponseEntity.ok(ApiResponse.success("My leave requests retrieved successfully", list));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getPendingLeaves(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<LeaveRequestResponse> list = leaveService.getPendingLeaves(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Pending leave requests retrieved successfully", list));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> getLeavesByEmployee(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<LeaveRequestResponse> list = leaveService.getLeavesByEmployee(employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee leave requests retrieved successfully", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getLeaveById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        LeaveRequestResponse response = leaveService.getLeaveById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Leave request retrieved successfully", response));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approveLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveActionRequest actionRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        LeaveRequestResponse response = leaveService.approveLeave(id, actionRequest, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Leave request approved successfully", response));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveActionRequest actionRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        LeaveRequestResponse response = leaveService.rejectLeave(id, actionRequest, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Leave request rejected successfully", response));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getMyLeaveBalances(
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<LeaveBalanceResponse> balances = leaveService.getMyLeaveBalances(year, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Leave balances retrieved successfully", balances));
    }

    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getLeaveBalancesByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<LeaveBalanceResponse> balances = leaveService.getLeaveBalancesByEmployee(employeeId, year, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee leave balances retrieved successfully", balances));
    }

    @PostMapping("/balance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> createLeaveBalance(
            @Valid @RequestBody LeaveBalanceCreateRequest request) {
        LeaveBalanceResponse response = leaveService.createLeaveBalance(request);
        return new ResponseEntity<>(ApiResponse.success("Leave balance created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/balance/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> updateLeaveBalance(
            @PathVariable Long id,
            @Valid @RequestBody LeaveBalanceUpdateRequest request) {
        LeaveBalanceResponse response = leaveService.updateLeaveBalance(id, request);
        return ResponseEntity.ok(ApiResponse.success("Leave balance updated successfully", response));
    }
}

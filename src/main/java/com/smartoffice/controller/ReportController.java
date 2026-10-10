package com.smartoffice.controller;

import com.smartoffice.dto.report.AdminDashboardResponse;
import com.smartoffice.dto.report.AttendanceAnalyticsResponse;
import com.smartoffice.dto.report.ComplaintAnalyticsResponse;
import com.smartoffice.dto.report.EmployeeDashboardResponse;
import com.smartoffice.dto.report.LeaveAnalyticsResponse;
import com.smartoffice.dto.report.ManagerDashboardResponse;
import com.smartoffice.dto.report.TaskAnalyticsResponse;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TaskAnalyticsResponse>> getTaskAnalytics(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TaskAnalyticsResponse response = reportService.getTaskAnalytics(departmentId, employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Task analytics retrieved successfully", response));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AttendanceAnalyticsResponse>> getAttendanceAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AttendanceAnalyticsResponse response = reportService.getAttendanceAnalytics(startDate, endDate, departmentId, employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Attendance analytics retrieved successfully", response));
    }

    @GetMapping("/leaves")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveAnalyticsResponse>> getLeaveAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        LeaveAnalyticsResponse response = reportService.getLeaveAnalytics(startDate, endDate, departmentId, employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Leave analytics retrieved successfully", response));
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ComplaintAnalyticsResponse>> getComplaintAnalytics(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintAnalyticsResponse response = reportService.getComplaintAnalytics(departmentId, employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaint analytics retrieved successfully", response));
    }

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AdminDashboardResponse response = reportService.getAdminDashboard(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard retrieved successfully", response));
    }

    @GetMapping("/dashboard/manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ManagerDashboardResponse>> getManagerDashboard(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ManagerDashboardResponse response = reportService.getManagerDashboard(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Manager dashboard retrieved successfully", response));
    }

    @GetMapping("/dashboard/employee")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeDashboardResponse>> getEmployeeDashboard(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeeDashboardResponse response = reportService.getEmployeeDashboard(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee dashboard retrieved successfully", response));
    }
}

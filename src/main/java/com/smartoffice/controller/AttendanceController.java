package com.smartoffice.controller;

import com.smartoffice.dto.attendance.AttendanceCreateRequest;
import com.smartoffice.dto.attendance.AttendanceResponse;
import com.smartoffice.dto.attendance.AttendanceSummaryResponse;
import com.smartoffice.dto.attendance.AttendanceUpdateRequest;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AttendanceResponse response = attendanceService.checkIn(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Checked in successfully", response));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AttendanceResponse response = attendanceService.checkOut(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Checked out successfully", response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AttendanceResponse> list = attendanceService.getMyAttendance(date, currentUser);
        return ResponseEntity.ok(ApiResponse.success("My attendance retrieved successfully", list));
    }

    @GetMapping("/my/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getMyMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AttendanceSummaryResponse summary = attendanceService.getMyMonthlySummary(year, month, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Monthly attendance summary retrieved successfully", summary));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AttendanceResponse> list = attendanceService.getEmployeeAttendance(employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee attendance retrieved successfully", list));
    }

    @GetMapping("/employee/{employeeId}/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getEmployeeMonthlyAttendance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AttendanceResponse> list = attendanceService.getEmployeeMonthlyAttendance(employeeId, year, month, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee monthly attendance retrieved successfully", list));
    }

    @GetMapping("/employee/{employeeId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getEmployeeMonthlySummary(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AttendanceSummaryResponse summary = attendanceService.getEmployeeMonthlySummary(employeeId, year, month, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee monthly summary retrieved successfully", summary));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AttendanceResponse> list = attendanceService.getAttendanceByDate(date, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Attendance by date retrieved successfully", list));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMonthlyAttendance(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AttendanceResponse> list = attendanceService.getMonthlyAttendance(year, month, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Monthly attendance retrieved successfully", list));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> createAttendanceRecord(
            @Valid @RequestBody AttendanceCreateRequest request) {
        AttendanceResponse response = attendanceService.createAttendanceRecord(request);
        return new ResponseEntity<>(ApiResponse.success("Attendance record created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendanceRecord(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceUpdateRequest request) {
        AttendanceResponse response = attendanceService.updateAttendanceRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success("Attendance record updated successfully", response));
    }
}

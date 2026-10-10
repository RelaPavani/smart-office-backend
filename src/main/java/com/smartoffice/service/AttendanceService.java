package com.smartoffice.service;

import com.smartoffice.dto.attendance.AttendanceCreateRequest;
import com.smartoffice.dto.attendance.AttendanceResponse;
import com.smartoffice.dto.attendance.AttendanceSummaryResponse;
import com.smartoffice.dto.attendance.AttendanceUpdateRequest;
import com.smartoffice.security.UserPrincipal;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponse checkIn(UserPrincipal currentUser);

    AttendanceResponse checkOut(UserPrincipal currentUser);

    List<AttendanceResponse> getMyAttendance(LocalDate date, UserPrincipal currentUser);

    AttendanceSummaryResponse getMyMonthlySummary(Integer year, Integer month, UserPrincipal currentUser);

    List<AttendanceResponse> getEmployeeAttendance(Long employeeId, UserPrincipal currentUser);

    List<AttendanceResponse> getEmployeeMonthlyAttendance(Long employeeId, Integer year, Integer month, UserPrincipal currentUser);

    AttendanceSummaryResponse getEmployeeMonthlySummary(Long employeeId, Integer year, Integer month, UserPrincipal currentUser);

    List<AttendanceResponse> getAttendanceByDate(LocalDate date, UserPrincipal currentUser);

    List<AttendanceResponse> getMonthlyAttendance(Integer year, Integer month, UserPrincipal currentUser);

    AttendanceResponse createAttendanceRecord(AttendanceCreateRequest request);

    AttendanceResponse updateAttendanceRecord(Long id, AttendanceUpdateRequest request);
}

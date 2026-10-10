package com.smartoffice.service;

import com.smartoffice.dto.report.AdminDashboardResponse;
import com.smartoffice.dto.report.AttendanceAnalyticsResponse;
import com.smartoffice.dto.report.ComplaintAnalyticsResponse;
import com.smartoffice.dto.report.EmployeeDashboardResponse;
import com.smartoffice.dto.report.LeaveAnalyticsResponse;
import com.smartoffice.dto.report.ManagerDashboardResponse;
import com.smartoffice.dto.report.TaskAnalyticsResponse;
import com.smartoffice.security.UserPrincipal;

import java.time.LocalDate;

public interface ReportService {

    TaskAnalyticsResponse getTaskAnalytics(Long departmentId, Long employeeId, UserPrincipal currentUser);

    AttendanceAnalyticsResponse getAttendanceAnalytics(LocalDate startDate, LocalDate endDate, Long departmentId, Long employeeId, UserPrincipal currentUser);

    LeaveAnalyticsResponse getLeaveAnalytics(LocalDate startDate, LocalDate endDate, Long departmentId, Long employeeId, UserPrincipal currentUser);

    ComplaintAnalyticsResponse getComplaintAnalytics(Long departmentId, Long employeeId, UserPrincipal currentUser);

    AdminDashboardResponse getAdminDashboard(UserPrincipal currentUser);

    ManagerDashboardResponse getManagerDashboard(UserPrincipal currentUser);

    EmployeeDashboardResponse getEmployeeDashboard(UserPrincipal currentUser);
}

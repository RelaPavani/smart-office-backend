package com.smartoffice.service.impl;

import com.smartoffice.dto.attendance.AttendanceCreateRequest;
import com.smartoffice.dto.attendance.AttendanceResponse;
import com.smartoffice.dto.attendance.AttendanceSummaryResponse;
import com.smartoffice.dto.attendance.AttendanceUpdateRequest;
import com.smartoffice.entity.Attendance;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.enums.AttendanceStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.AttendanceRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.AttendanceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final LocalTime STANDARD_WORK_START = LocalTime.of(9, 30);
    private static final long MIN_FULL_DAY_MINUTES = 240; // 4 hours

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceServiceImpl(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);
        LocalDate today = LocalDate.now();

        var existingOpt = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today);
        if (existingOpt.isPresent() && existingOpt.get().getCheckIn() != null) {
            throw new ConflictException("Employee has already checked in today (" + today + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        AttendanceStatus status = now.toLocalTime().isAfter(STANDARD_WORK_START)
                ? AttendanceStatus.LATE
                : AttendanceStatus.PRESENT;

        Attendance attendance = existingOpt.orElseGet(() -> Attendance.builder()
                .employee(employee)
                .attendanceDate(today)
                .build());

        attendance.setCheckIn(now);
        attendance.setStatus(status);

        Attendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new BadRequestException("Cannot check out without checking in first for date: " + today));

        if (attendance.getCheckIn() == null) {
            throw new BadRequestException("Cannot check out without checking in first");
        }

        if (attendance.getCheckOut() != null) {
            throw new ConflictException("Employee has already checked out today (" + today + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOut(now);

        long durationMinutes = Duration.between(attendance.getCheckIn(), now).toMinutes();
        if (durationMinutes < MIN_FULL_DAY_MINUTES && attendance.getStatus() == AttendanceStatus.PRESENT) {
            attendance.setStatus(AttendanceStatus.HALF_DAY);
        }

        Attendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance(LocalDate date, UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);
        if (date != null) {
            return attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), date)
                    .map(this::mapToResponse)
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }
        return attendanceRepository.findByEmployeeId(employee.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getMyMonthlySummary(Integer year, Integer month, UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);
        return computeMonthlySummary(employee, year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getEmployeeAttendance(Long employeeId, UserPrincipal currentUser) {
        validateEmployeeAccess(currentUser, employeeId);
        return attendanceRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getEmployeeMonthlyAttendance(
            Long employeeId,
            Integer year,
            Integer month,
            UserPrincipal currentUser) {
        validateEmployeeAccess(currentUser, employeeId);

        YearMonth ym = resolveYearMonth(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, start, end).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getEmployeeMonthlySummary(
            Long employeeId,
            Integer year,
            Integer month,
            UserPrincipal currentUser) {
        validateEmployeeAccess(currentUser, employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        return computeMonthlySummary(employee, year, month);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date, UserPrincipal currentUser) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        if (currentUser.getRole() == Role.ADMIN) {
            return attendanceRepository.findByAttendanceDate(queryDate).stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (currentUser.getRole() == Role.MANAGER) {
            Employee manager = resolveCurrentEmployee(currentUser);
            if (manager.getDepartment() != null) {
                return attendanceRepository.findByEmployeeDepartmentIdAndAttendanceDate(
                        manager.getDepartment().getId(), queryDate).stream()
                        .map(this::mapToResponse)
                        .toList();
            }
        }

        throw new AccessDeniedException("Access denied: You cannot view attendance by date");
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMonthlyAttendance(Integer year, Integer month, UserPrincipal currentUser) {
        YearMonth ym = resolveYearMonth(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        if (currentUser.getRole() == Role.ADMIN) {
            return attendanceRepository.findByAttendanceDateBetween(start, end).stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (currentUser.getRole() == Role.MANAGER) {
            Employee manager = resolveCurrentEmployee(currentUser);
            if (manager.getDepartment() != null) {
                return attendanceRepository.findByEmployeeDepartmentIdAndAttendanceDateBetween(
                        manager.getDepartment().getId(), start, end).stream()
                        .map(this::mapToResponse)
                        .toList();
            }
        }

        throw new AccessDeniedException("Access denied: You cannot view monthly attendance");
    }

    @Override
    @Transactional
    public AttendanceResponse createAttendanceRecord(AttendanceCreateRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        if (attendanceRepository.existsByEmployeeIdAndAttendanceDate(request.getEmployeeId(), request.getAttendanceDate())) {
            throw new ConflictException("Attendance already exists for employee " + request.getEmployeeId() + " on " + request.getAttendanceDate());
        }

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .attendanceDate(request.getAttendanceDate())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .status(request.getStatus())
                .build();

        Attendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AttendanceResponse updateAttendanceRecord(Long id, AttendanceUpdateRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));

        if (request.getCheckIn() != null) {
            attendance.setCheckIn(request.getCheckIn());
        }
        if (request.getCheckOut() != null) {
            attendance.setCheckOut(request.getCheckOut());
        }
        if (request.getStatus() != null) {
            attendance.setStatus(request.getStatus());
        }

        Attendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    private AttendanceSummaryResponse computeMonthlySummary(Employee employee, Integer year, Integer month) {
        YearMonth ym = resolveYearMonth(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        LocalDate effectiveEnd = (ym.getYear() == today.getYear() && ym.getMonthValue() == today.getMonthValue())
                ? today
                : end;

        int workingDays = 0;
        LocalDate current = start;
        while (!current.isAfter(effectiveEnd)) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(employee.getId(), start, end);

        int presentDays = 0;
        int lateDays = 0;
        int halfDays = 0;

        for (Attendance att : attendances) {
            if (att.getStatus() == AttendanceStatus.PRESENT) {
                presentDays++;
            } else if (att.getStatus() == AttendanceStatus.LATE) {
                lateDays++;
            } else if (att.getStatus() == AttendanceStatus.HALF_DAY) {
                halfDays++;
            }
        }

        int attendedDays = presentDays + lateDays + halfDays;
        int absentDays = Math.max(0, workingDays - attendedDays);
        double percentage = workingDays > 0
                ? Math.round(((double) attendedDays / workingDays * 100.0) * 10.0) / 10.0
                : 0.0;

        return AttendanceSummaryResponse.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .year(ym.getYear())
                .month(ym.getMonthValue())
                .totalWorkingDays(workingDays)
                .presentDays(presentDays)
                .absentDays(absentDays)
                .lateDays(lateDays)
                .halfDays(halfDays)
                .attendancePercentage(percentage)
                .build();
    }

    private void validateEmployeeAccess(UserPrincipal currentUser, Long targetEmployeeId) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        Employee currentEmp = resolveCurrentEmployee(currentUser);
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (!targetEmployeeId.equals(currentEmp.getId())) {
                throw new AccessDeniedException("Access denied: You can only view your own attendance");
            }
            return;
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (targetEmployeeId.equals(currentEmp.getId())) {
                return;
            }
            Employee target = employeeRepository.findById(targetEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", targetEmployeeId));

            boolean isDirectReport = target.getManager() != null && target.getManager().getId().equals(currentEmp.getId());
            boolean isSameDept = target.getDepartment() != null && currentEmp.getDepartment() != null
                    && target.getDepartment().getId().equals(currentEmp.getDepartment().getId());

            if (!isDirectReport && !isSameDept) {
                throw new AccessDeniedException("Access denied: You can only view attendance for your team or department");
            }
        }
    }

    private Employee resolveCurrentEmployee(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile not found for user: " + currentUser.getUsername()));
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {
        int y = (year != null && year > 0) ? year : LocalDate.now().getYear();
        int m = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();
        return YearMonth.of(y, m);
    }

    private AttendanceResponse mapToResponse(Attendance att) {
        Double workingHours = null;
        if (att.getCheckIn() != null && att.getCheckOut() != null) {
            long minutes = Duration.between(att.getCheckIn(), att.getCheckOut()).toMinutes();
            workingHours = Math.round((minutes / 60.0) * 10.0) / 10.0;
        }

        return AttendanceResponse.builder()
                .id(att.getId())
                .employeeId(att.getEmployee().getId())
                .employeeName(att.getEmployee().getFirstName() + " " + att.getEmployee().getLastName())
                .employeeCode(att.getEmployee().getEmployeeCode())
                .attendanceDate(att.getAttendanceDate())
                .checkIn(att.getCheckIn())
                .checkOut(att.getCheckOut())
                .status(att.getStatus())
                .workingHours(workingHours)
                .createdAt(att.getCreatedAt())
                .updatedAt(att.getUpdatedAt())
                .build();
    }
}

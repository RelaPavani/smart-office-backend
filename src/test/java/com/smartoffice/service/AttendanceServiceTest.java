package com.smartoffice.service;

import com.smartoffice.dto.attendance.AttendanceResponse;
import com.smartoffice.dto.attendance.AttendanceSummaryResponse;
import com.smartoffice.entity.Attendance;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.enums.AttendanceStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.repository.AttendanceRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Employee employee1;
    private Employee employee2;
    private UserPrincipal employee1Principal;
    private UserPrincipal employee2Principal;

    @BeforeEach
    void setUp() {
        employee1 = Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@smartoffice.com")
                .build();

        employee2 = Employee.builder()
                .id(2L)
                .employeeCode("EMP002")
                .firstName("Bob")
                .lastName("Jones")
                .email("bob@smartoffice.com")
                .build();

        employee1Principal = new UserPrincipal(
                101L, "alice", "alice@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList()
        );

        employee2Principal = new UserPrincipal(
                102L, "bob", "bob@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Employee check-in succeeds")
    void testCheckInSuccess() {
        LocalDate today = LocalDate.now();
        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> {
            Attendance att = i.getArgument(0);
            att.setId(10L);
            return att;
        });

        AttendanceResponse response = attendanceService.checkIn(employee1Principal);

        assertNotNull(response);
        assertNotNull(response.getCheckIn());
        assertEquals(today, response.getAttendanceDate());
        assertEquals(1L, response.getEmployeeId());
    }

    @Test
    @DisplayName("Duplicate check-in on the same day is rejected with ConflictException")
    void testDuplicateCheckInRejected() {
        LocalDate today = LocalDate.now();
        Attendance existing = Attendance.builder()
                .id(10L)
                .employee(employee1)
                .attendanceDate(today)
                .checkIn(LocalDateTime.now().minusHours(2))
                .status(AttendanceStatus.PRESENT)
                .build();

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> attendanceService.checkIn(employee1Principal));
    }

    @Test
    @DisplayName("Check-out succeeds after check-in")
    void testCheckOutSuccess() {
        LocalDate today = LocalDate.now();
        Attendance existing = Attendance.builder()
                .id(10L)
                .employee(employee1)
                .attendanceDate(today)
                .checkIn(LocalDateTime.now().minusHours(8))
                .status(AttendanceStatus.PRESENT)
                .build();

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));

        AttendanceResponse response = attendanceService.checkOut(employee1Principal);

        assertNotNull(response);
        assertNotNull(response.getCheckOut());
        assertNotNull(response.getWorkingHours());
    }

    @Test
    @DisplayName("Check-out without check-in is rejected with BadRequestException")
    void testCheckOutWithoutCheckInRejected() {
        LocalDate today = LocalDate.now();
        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> attendanceService.checkOut(employee1Principal));
    }

    @Test
    @DisplayName("Duplicate check-out is rejected with ConflictException")
    void testDuplicateCheckOutRejected() {
        LocalDate today = LocalDate.now();
        Attendance existing = Attendance.builder()
                .id(10L)
                .employee(employee1)
                .attendanceDate(today)
                .checkIn(LocalDateTime.now().minusHours(8))
                .checkOut(LocalDateTime.now().minusMinutes(5))
                .status(AttendanceStatus.PRESENT)
                .build();

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDate(1L, today)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> attendanceService.checkOut(employee1Principal));
    }

    @Test
    @DisplayName("Employee can view own attendance")
    void testEmployeeViewsOwnAttendance() {
        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(attendanceRepository.findByEmployeeId(1L)).thenReturn(List.of(
                Attendance.builder()
                        .id(1L)
                        .employee(employee1)
                        .attendanceDate(LocalDate.now().minusDays(1))
                        .checkIn(LocalDateTime.now().minusDays(1))
                        .status(AttendanceStatus.PRESENT)
                        .build()
        ));

        List<AttendanceResponse> list = attendanceService.getMyAttendance(null, employee1Principal);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getEmployeeId());
    }

    @Test
    @DisplayName("Unauthorized employee cannot view another employee's attendance")
    void testUnauthorizedEmployeeCannotViewOthersAttendance() {
        when(employeeRepository.findByUserId(102L)).thenReturn(Optional.of(employee2));

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.getEmployeeAttendance(1L, employee2Principal));
    }

    @Test
    @DisplayName("Monthly attendance summary computes metrics correctly")
    void testMonthlySummaryMetrics() {
        int year = 2026;
        int month = 10;
        LocalDate d1 = LocalDate.of(year, month, 1);
        LocalDate d2 = LocalDate.of(year, month, 2);

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        Attendance.builder().id(1L).employee(employee1).attendanceDate(d1).status(AttendanceStatus.PRESENT).build(),
                        Attendance.builder().id(2L).employee(employee1).attendanceDate(d2).status(AttendanceStatus.LATE).build()
                ));

        AttendanceSummaryResponse summary = attendanceService.getMyMonthlySummary(year, month, employee1Principal);

        assertNotNull(summary);
        assertEquals(1, summary.getPresentDays());
        assertEquals(1, summary.getLateDays());
        assertTrue(summary.getTotalWorkingDays() >= 2);
        assertTrue(summary.getAttendancePercentage() > 0);
    }
}

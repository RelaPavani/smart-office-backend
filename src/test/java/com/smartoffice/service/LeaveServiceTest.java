package com.smartoffice.service;

import com.smartoffice.dto.leave.LeaveActionRequest;
import com.smartoffice.dto.leave.LeaveRequestCreateRequest;
import com.smartoffice.dto.leave.LeaveRequestResponse;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.LeaveBalance;
import com.smartoffice.entity.LeaveRequest;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.LeaveStatus;
import com.smartoffice.entity.enums.LeaveType;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.LeaveBalanceRepository;
import com.smartoffice.repository.LeaveRequestRepository;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.LeaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private Employee employee1;
    private Employee managerEmployee;
    private User employeeUser;
    private User managerUser;
    private User adminUser;
    private UserPrincipal employeePrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal adminPrincipal;
    private LeaveBalance leaveBalance;

    @BeforeEach
    void setUp() {
        employeeUser = User.builder().id(101L).username("emp1").email("emp1@smartoffice.com").role(Role.EMPLOYEE).build();
        managerUser = User.builder().id(102L).username("mgr1").email("mgr1@smartoffice.com").role(Role.MANAGER).build();
        adminUser = User.builder().id(999L).username("admin").email("admin@smartoffice.com").role(Role.ADMIN).build();

        managerEmployee = Employee.builder()
                .id(2L)
                .employeeCode("MGR001")
                .firstName("Boss")
                .lastName("Man")
                .email("mgr1@smartoffice.com")
                .user(managerUser)
                .build();

        employee1 = Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .firstName("Alice")
                .lastName("Smith")
                .email("emp1@smartoffice.com")
                .user(employeeUser)
                .manager(managerEmployee)
                .build();

        employeePrincipal = new UserPrincipal(101L, "emp1", "emp1@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());
        managerPrincipal = new UserPrincipal(102L, "mgr1", "mgr1@smartoffice.com", "pass", Role.MANAGER, true, Collections.emptyList());
        adminPrincipal = new UserPrincipal(999L, "admin", "admin@smartoffice.com", "pass", Role.ADMIN, true, Collections.emptyList());

        leaveBalance = LeaveBalance.builder()
                .id(1L)
                .employee(employee1)
                .year(2026)
                .leaveType(LeaveType.CASUAL)
                .totalLeaves(12)
                .usedLeaves(2)
                .remainingLeaves(10)
                .build();
    }

    @Test
    @DisplayName("Apply for leave successfully with status PENDING")
    void testApplyForLeaveSuccess() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 3); // 3 days

        LeaveRequestCreateRequest request = LeaveRequestCreateRequest.builder()
                .leaveType(LeaveType.CASUAL)
                .startDate(start)
                .endDate(end)
                .reason("Vacation trip")
                .build();

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(leaveRequestRepository.findOverlappingLeaves(eq(1L), eq(start), eq(end), any()))
                .thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(1L, 2026, LeaveType.CASUAL)).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(1L, 2026, LeaveType.CASUAL))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> {
            LeaveRequest lr = i.getArgument(0);
            lr.setId(50L);
            return lr;
        });

        LeaveRequestResponse response = leaveService.applyForLeave(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(LeaveStatus.PENDING, response.getStatus());
        assertEquals(3, response.getDays());
        assertEquals(LeaveType.CASUAL, response.getLeaveType());
    }

    @Test
    @DisplayName("Invalid date range (end date before start date) throws BadRequestException")
    void testInvalidDateRangeThrowsBadRequest() {
        LeaveRequestCreateRequest request = LeaveRequestCreateRequest.builder()
                .leaveType(LeaveType.CASUAL)
                .startDate(LocalDate.of(2026, 11, 5))
                .endDate(LocalDate.of(2026, 11, 1))
                .reason("Trip")
                .build();

        assertThrows(BadRequestException.class, () -> leaveService.applyForLeave(request, employeePrincipal));
    }

    @Test
    @DisplayName("Overlapping leave request throws ConflictException")
    void testOverlappingLeaveThrowsConflict() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 3);

        LeaveRequestCreateRequest request = LeaveRequestCreateRequest.builder()
                .leaveType(LeaveType.CASUAL)
                .startDate(start)
                .endDate(end)
                .reason("Trip")
                .build();

        LeaveRequest existing = LeaveRequest.builder().id(10L).startDate(start).endDate(end).status(LeaveStatus.PENDING).build();

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(leaveRequestRepository.findOverlappingLeaves(eq(1L), eq(start), eq(end), any()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> leaveService.applyForLeave(request, employeePrincipal));
    }

    @Test
    @DisplayName("Insufficient leave balance throws BadRequestException")
    void testInsufficientBalanceThrowsBadRequest() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 20); // 20 days requested, only 10 remaining

        LeaveRequestCreateRequest request = LeaveRequestCreateRequest.builder()
                .leaveType(LeaveType.CASUAL)
                .startDate(start)
                .endDate(end)
                .reason("Long trip")
                .build();

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(leaveRequestRepository.findOverlappingLeaves(eq(1L), eq(start), eq(end), any()))
                .thenReturn(Collections.emptyList());
        when(leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(1L, 2026, LeaveType.CASUAL)).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(1L, 2026, LeaveType.CASUAL))
                .thenReturn(Optional.of(leaveBalance));

        assertThrows(BadRequestException.class, () -> leaveService.applyForLeave(request, employeePrincipal));
    }

    @Test
    @DisplayName("Approve leave deducts leave balance and sets status APPROVED")
    void testApproveLeaveDeductsBalance() {
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(50L)
                .employee(employee1)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 3)) // 3 days
                .leaveType(LeaveType.CASUAL)
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leaveRequest));
        when(employeeRepository.findByUserId(102L)).thenReturn(Optional.of(managerEmployee));
        when(userRepository.findById(102L)).thenReturn(Optional.of(managerUser));
        when(leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(1L, 2026, LeaveType.CASUAL)).thenReturn(true);
        when(leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(1L, 2026, LeaveType.CASUAL))
                .thenReturn(Optional.of(leaveBalance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequestResponse response = leaveService.approveLeave(50L, new LeaveActionRequest("Approved"), managerPrincipal);

        assertNotNull(response);
        assertEquals(LeaveStatus.APPROVED, response.getStatus());
        assertEquals(5, leaveBalance.getUsedLeaves()); // 2 + 3
        assertEquals(7, leaveBalance.getRemainingLeaves()); // 12 - 5
        verify(leaveBalanceRepository).save(leaveBalance);
    }

    @Test
    @DisplayName("Reject leave does NOT deduct balance and sets status REJECTED")
    void testRejectLeaveDoesNotDeductBalance() {
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(50L)
                .employee(employee1)
                .startDate(LocalDate.of(2026, 11, 1))
                .endDate(LocalDate.of(2026, 11, 3))
                .leaveType(LeaveType.CASUAL)
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leaveRequest));
        when(employeeRepository.findByUserId(102L)).thenReturn(Optional.of(managerEmployee));
        when(userRepository.findById(102L)).thenReturn(Optional.of(managerUser));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveRequestResponse response = leaveService.rejectLeave(50L, new LeaveActionRequest("Critical deliverable"), managerPrincipal);

        assertNotNull(response);
        assertEquals(LeaveStatus.REJECTED, response.getStatus());
        assertEquals("Critical deliverable", response.getRejectionReason());
        verify(leaveBalanceRepository, never()).save(any(LeaveBalance.class));
    }

    @Test
    @DisplayName("Double approval protection: Cannot approve an already approved leave")
    void testDoubleApprovalProtection() {
        LeaveRequest approvedLeave = LeaveRequest.builder()
                .id(50L)
                .employee(employee1)
                .status(LeaveStatus.APPROVED)
                .build();

        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(approvedLeave));

        assertThrows(BadRequestException.class,
                () -> leaveService.approveLeave(50L, new LeaveActionRequest(), managerPrincipal));
    }

    @Test
    @DisplayName("Employee cannot approve their own leave request")
    void testEmployeeCannotApproveOwnLeave() {
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .id(50L)
                .employee(employee1)
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leaveRequest));
        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));

        assertThrows(BadRequestException.class,
                () -> leaveService.approveLeave(50L, new LeaveActionRequest(), employeePrincipal));
    }

    @Test
    @DisplayName("Unauthorized employee cannot view another employee's leaves")
    void testUnauthorizedEmployeeCannotViewOthersLeaves() {
        Employee employee2 = Employee.builder().id(2L).build();
        UserPrincipal emp2Principal = new UserPrincipal(102L, "emp2", "emp2@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());

        when(employeeRepository.findByUserId(102L)).thenReturn(Optional.of(employee2));

        assertThrows(AccessDeniedException.class,
                () -> leaveService.getLeavesByEmployee(1L, emp2Principal));
    }
}

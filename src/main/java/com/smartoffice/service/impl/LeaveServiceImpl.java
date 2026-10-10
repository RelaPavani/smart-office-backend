package com.smartoffice.service.impl;

import com.smartoffice.dto.leave.LeaveActionRequest;
import com.smartoffice.dto.leave.LeaveBalanceCreateRequest;
import com.smartoffice.dto.leave.LeaveBalanceResponse;
import com.smartoffice.dto.leave.LeaveBalanceUpdateRequest;
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
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.LeaveBalanceRepository;
import com.smartoffice.repository.LeaveRequestRepository;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.LeaveService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public LeaveServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            LeaveBalanceRepository leaveBalanceRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public LeaveRequestResponse applyForLeave(LeaveRequestCreateRequest request, UserPrincipal currentUser) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Employee employee = resolveTargetEmployee(request.getEmployeeId(), currentUser);
        int requestedDays = calculateDays(request.getStartDate(), request.getEndDate());

        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingLeaves(
                employee.getId(),
                request.getStartDate(),
                request.getEndDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException("An overlapping leave request already exists for the specified dates");
        }

        int year = request.getStartDate().getYear();
        LeaveBalance balance = getOrInitializeBalance(employee, year, request.getLeaveType());

        if (balance.getRemainingLeaves() < requestedDays) {
            throw new BadRequestException("Insufficient leave balance. Requested: " + requestedDays
                    + ", Remaining: " + balance.getRemainingLeaves());
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason().trim())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMyLeaves(UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);
        return leaveRequestRepository.findByEmployeeId(employee.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveById(Long id, UserPrincipal currentUser) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        validateEmployeeAccess(currentUser, leaveRequest.getEmployee().getId());
        return mapToResponse(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingLeaves(UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return leaveRequestRepository.findByStatus(LeaveStatus.PENDING).stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (currentUser.getRole() == Role.MANAGER) {
            Employee manager = resolveCurrentEmployee(currentUser);
            if (manager.getDepartment() != null) {
                return leaveRequestRepository.findByEmployeeDepartmentIdAndStatus(
                        manager.getDepartment().getId(), LeaveStatus.PENDING).stream()
                        .map(this::mapToResponse)
                        .toList();
            }
        }

        throw new AccessDeniedException("Access denied: You do not have permission to view pending leaves");
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeavesByEmployee(Long employeeId, UserPrincipal currentUser) {
        validateEmployeeAccess(currentUser, employeeId);
        return leaveRequestRepository.findByEmployeeId(employeeId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveRequestResponse approveLeave(Long id, LeaveActionRequest actionRequest, UserPrincipal currentUser) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Cannot approve leave request with status: " + leaveRequest.getStatus());
        }

        validateReviewerPermissions(leaveRequest, currentUser);

        int requestedDays = calculateDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());
        int year = leaveRequest.getStartDate().getYear();

        LeaveBalance balance = getOrInitializeBalance(leaveRequest.getEmployee(), year, leaveRequest.getLeaveType());

        if (balance.getRemainingLeaves() < requestedDays) {
            throw new BadRequestException("Insufficient leave balance for approval. Requested: " + requestedDays
                    + ", Remaining: " + balance.getRemainingLeaves());
        }

        balance.setUsedLeaves(balance.getUsedLeaves() + requestedDays);
        balance.setRemainingLeaves(balance.getTotalLeaves() - balance.getUsedLeaves());
        leaveBalanceRepository.save(balance);

        User reviewer = userRepository.findById(currentUser.getId())
                .orElse(null);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setReviewedBy(reviewer);
        leaveRequest.setReviewedAt(LocalDateTime.now());

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public LeaveRequestResponse rejectLeave(Long id, LeaveActionRequest actionRequest, UserPrincipal currentUser) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Cannot reject leave request with status: " + leaveRequest.getStatus());
        }

        validateReviewerPermissions(leaveRequest, currentUser);

        User reviewer = userRepository.findById(currentUser.getId())
                .orElse(null);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setReviewedBy(reviewer);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        if (actionRequest != null && actionRequest.getRemarks() != null) {
            leaveRequest.setRejectionReason(actionRequest.getRemarks().trim());
        }

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public List<LeaveBalanceResponse> getMyLeaveBalances(Integer year, UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);
        int targetYear = (year != null && year > 0) ? year : LocalDate.now().getYear();
        ensureDefaultBalances(employee, targetYear);

        return leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), targetYear).stream()
                .map(this::mapToBalanceResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<LeaveBalanceResponse> getLeaveBalancesByEmployee(Long employeeId, Integer year, UserPrincipal currentUser) {
        validateEmployeeAccess(currentUser, employeeId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        int targetYear = (year != null && year > 0) ? year : LocalDate.now().getYear();
        ensureDefaultBalances(employee, targetYear);

        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, targetYear).stream()
                .map(this::mapToBalanceResponse)
                .toList();
    }

    @Override
    @Transactional
    public LeaveBalanceResponse createLeaveBalance(LeaveBalanceCreateRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));

        if (leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(
                request.getEmployeeId(), request.getYear(), request.getLeaveType())) {
            throw new ConflictException("Leave balance already exists for year " + request.getYear()
                    + " and type " + request.getLeaveType());
        }

        int used = (request.getUsedLeaves() != null) ? request.getUsedLeaves() : 0;
        int remaining = (request.getRemainingLeaves() != null)
                ? request.getRemainingLeaves()
                : (request.getTotalLeaves() - used);

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .year(request.getYear())
                .leaveType(request.getLeaveType())
                .totalLeaves(request.getTotalLeaves())
                .usedLeaves(used)
                .remainingLeaves(remaining)
                .build();

        LeaveBalance saved = leaveBalanceRepository.save(balance);
        return mapToBalanceResponse(saved);
    }

    @Override
    @Transactional
    public LeaveBalanceResponse updateLeaveBalance(Long id, LeaveBalanceUpdateRequest request) {
        LeaveBalance balance = leaveBalanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", "id", id));

        if (request.getTotalLeaves() != null) {
            balance.setTotalLeaves(request.getTotalLeaves());
        }
        if (request.getUsedLeaves() != null) {
            balance.setUsedLeaves(request.getUsedLeaves());
        }
        if (request.getRemainingLeaves() != null) {
            balance.setRemainingLeaves(request.getRemainingLeaves());
        } else if (request.getTotalLeaves() != null || request.getUsedLeaves() != null) {
            balance.setRemainingLeaves(balance.getTotalLeaves() - balance.getUsedLeaves());
        }

        LeaveBalance saved = leaveBalanceRepository.save(balance);
        return mapToBalanceResponse(saved);
    }

    private void validateReviewerPermissions(LeaveRequest leaveRequest, UserPrincipal currentUser) {
        // Employee cannot approve or reject their own leave
        Employee currentEmp = employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElse(null);

        if (currentEmp != null && leaveRequest.getEmployee().getId().equals(currentEmp.getId())) {
            throw new BadRequestException("Employee cannot approve or reject their own leave request");
        }

        if (leaveRequest.getEmployee().getUser() != null
                && leaveRequest.getEmployee().getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Employee cannot approve or reject their own leave request");
        }

        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (currentEmp == null) {
                throw new AccessDeniedException("Manager employee record not found");
            }

            Employee applicant = leaveRequest.getEmployee();
            boolean isDirectReport = applicant.getManager() != null
                    && applicant.getManager().getId().equals(currentEmp.getId());
            boolean isSameDept = applicant.getDepartment() != null
                    && currentEmp.getDepartment() != null
                    && applicant.getDepartment().getId().equals(currentEmp.getDepartment().getId());

            if (!isDirectReport && !isSameDept) {
                throw new AccessDeniedException("Access denied: You can only review leave requests for your team members");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: You do not have permission to approve or reject leave requests");
    }

    private void validateEmployeeAccess(UserPrincipal currentUser, Long targetEmployeeId) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        Employee currentEmp = resolveCurrentEmployee(currentUser);
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (!targetEmployeeId.equals(currentEmp.getId())) {
                throw new AccessDeniedException("Access denied: You can only view your own leave requests");
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
                throw new AccessDeniedException("Access denied: You can only view leaves for your team or department");
            }
        }
    }

    private Employee resolveTargetEmployee(Long requestedEmployeeId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN && requestedEmployeeId != null) {
            return employeeRepository.findById(requestedEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", requestedEmployeeId));
        }

        Employee currentEmp = resolveCurrentEmployee(currentUser);
        if (requestedEmployeeId != null && !requestedEmployeeId.equals(currentEmp.getId())) {
            throw new AccessDeniedException("Access denied: You can only submit leave requests for yourself");
        }
        return currentEmp;
    }

    private Employee resolveCurrentEmployee(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile not found for user: " + currentUser.getUsername()));
    }

    private int calculateDays(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    private LeaveBalance getOrInitializeBalance(Employee employee, int year, LeaveType leaveType) {
        ensureDefaultBalances(employee, year);
        return leaveBalanceRepository.findByEmployeeIdAndYearAndLeaveType(employee.getId(), year, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found for type: " + leaveType));
    }

    private void ensureDefaultBalances(Employee employee, int year) {
        if (!leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(employee.getId(), year, LeaveType.CASUAL)) {
            leaveBalanceRepository.save(createDefaultBalance(employee, year, LeaveType.CASUAL, 12));
        }
        if (!leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(employee.getId(), year, LeaveType.SICK)) {
            leaveBalanceRepository.save(createDefaultBalance(employee, year, LeaveType.SICK, 10));
        }
        if (!leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(employee.getId(), year, LeaveType.EARNED)) {
            leaveBalanceRepository.save(createDefaultBalance(employee, year, LeaveType.EARNED, 15));
        }
        if (!leaveBalanceRepository.existsByEmployeeIdAndYearAndLeaveType(employee.getId(), year, LeaveType.OTHER)) {
            leaveBalanceRepository.save(createDefaultBalance(employee, year, LeaveType.OTHER, 5));
        }
    }

    private LeaveBalance createDefaultBalance(Employee employee, int year, LeaveType type, int total) {
        return LeaveBalance.builder()
                .employee(employee)
                .year(year)
                .leaveType(type)
                .totalLeaves(total)
                .usedLeaves(0)
                .remainingLeaves(total)
                .build();
    }

    private LeaveRequestResponse mapToResponse(LeaveRequest lr) {
        int days = calculateDays(lr.getStartDate(), lr.getEndDate());

        return LeaveRequestResponse.builder()
                .id(lr.getId())
                .employeeId(lr.getEmployee().getId())
                .employeeName(lr.getEmployee().getFirstName() + " " + lr.getEmployee().getLastName())
                .employeeCode(lr.getEmployee().getEmployeeCode())
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .days(days)
                .reason(lr.getReason())
                .status(lr.getStatus())
                .reviewedByUserId(lr.getReviewedBy() != null ? lr.getReviewedBy().getId() : null)
                .reviewedByUsername(lr.getReviewedBy() != null ? lr.getReviewedBy().getUsername() : null)
                .reviewedAt(lr.getReviewedAt())
                .rejectionReason(lr.getRejectionReason())
                .createdAt(lr.getCreatedAt())
                .updatedAt(lr.getUpdatedAt())
                .build();
    }

    private LeaveBalanceResponse mapToBalanceResponse(LeaveBalance lb) {
        return LeaveBalanceResponse.builder()
                .id(lb.getId())
                .employeeId(lb.getEmployee().getId())
                .employeeName(lb.getEmployee().getFirstName() + " " + lb.getEmployee().getLastName())
                .year(lb.getYear())
                .leaveType(lb.getLeaveType())
                .totalLeaves(lb.getTotalLeaves())
                .usedLeaves(lb.getUsedLeaves())
                .remainingLeaves(lb.getRemainingLeaves())
                .createdAt(lb.getCreatedAt())
                .updatedAt(lb.getUpdatedAt())
                .build();
    }
}

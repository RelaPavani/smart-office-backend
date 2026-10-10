package com.smartoffice.service.impl;

import com.smartoffice.dto.complaint.ComplaintCreateRequest;
import com.smartoffice.dto.complaint.ComplaintResponse;
import com.smartoffice.dto.complaint.ComplaintUpdateRequest;
import com.smartoffice.entity.Complaint;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.ComplaintRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.ComplaintService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final EmployeeRepository employeeRepository;

    public ComplaintServiceImpl(ComplaintRepository complaintRepository, EmployeeRepository employeeRepository) {
        this.complaintRepository = complaintRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public ComplaintResponse createComplaint(ComplaintCreateRequest request, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        Employee requester;
        if (currentUser.getRole() == Role.EMPLOYEE || currentUser.getRole() == Role.MANAGER) {
            if (currentEmp == null) {
                throw new AccessDeniedException("Employee profile not found for user");
            }
            if (request.getEmployeeId() != null && !request.getEmployeeId().equals(currentEmp.getId())) {
                throw new AccessDeniedException("Access denied: You cannot submit a complaint on behalf of another employee");
            }
            requester = currentEmp;
        } else {
            // ADMIN
            if (request.getEmployeeId() != null) {
                requester = employeeRepository.findById(request.getEmployeeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
            } else if (currentEmp != null) {
                requester = currentEmp;
            } else {
                throw new BadRequestException("Employee ID is required");
            }
        }

        if (requester.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Employee must be active to file complaints");
        }

        Complaint complaint = Complaint.builder()
                .employee(requester)
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(ComplaintStatus.OPEN)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplaintResponse getComplaintById(Long id, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        validateComplaintReadAccess(complaint, currentUser);

        return mapToResponse(complaint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> listComplaints(ComplaintStatus status, ComplaintCategory category, ComplaintPriority priority, Long employeeId, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        List<Complaint> allComplaints = complaintRepository.findAll();

        return allComplaints.stream()
                .filter(c -> hasComplaintReadPermission(c, currentUser, currentEmp))
                .filter(c -> status == null || c.getStatus() == status)
                .filter(c -> category == null || c.getCategory() == category)
                .filter(c -> priority == null || c.getPriority() == priority)
                .filter(c -> employeeId == null || c.getEmployee().getId().equals(employeeId))
                .sorted(Comparator.comparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getMyComplaints(UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployee(currentUser);

        List<Complaint> list = complaintRepository.findByEmployeeId(currentEmp.getId());
        return list.stream()
                .sorted(Comparator.comparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComplaintResponse updateComplaint(Long id, ComplaintUpdateRequest request, UserPrincipal currentUser) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (currentEmp == null || !complaint.getEmployee().getId().equals(currentEmp.getId())) {
                throw new AccessDeniedException("Access denied: You can only update your own complaints");
            }
            if (complaint.getStatus() != ComplaintStatus.OPEN) {
                throw new BadRequestException("Cannot modify complaint that is already in progress or resolved");
            }
            if (request.getCategory() != null) {
                complaint.setCategory(request.getCategory());
            }
            if (request.getPriority() != null) {
                complaint.setPriority(request.getPriority());
            }
            // Employees cannot set assignedTo, status, or resolution
            if (request.getAssignedToId() != null || request.getStatus() != null || request.getResolution() != null) {
                throw new AccessDeniedException("Access denied: Employees cannot assign, change status, or resolve complaints");
            }
        } else {
            // ADMIN or MANAGER
            if (currentUser.getRole() == Role.MANAGER) {
                validateManagerComplaintScope(complaint, currentEmp);
            }

            if (request.getCategory() != null) {
                complaint.setCategory(request.getCategory());
            }
            if (request.getPriority() != null) {
                complaint.setPriority(request.getPriority());
            }
            if (request.getAssignedToId() != null) {
                Employee assignee = employeeRepository.findById(request.getAssignedToId())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getAssignedToId()));
                if (assignee.getStatus() != EmployeeStatus.ACTIVE) {
                    throw new BadRequestException("Cannot assign complaint to an inactive employee");
                }
                if (currentUser.getRole() == Role.MANAGER && !isWithinManagerScope(currentEmp, assignee)) {
                    throw new AccessDeniedException("Access denied: Managers can only assign complaints to staff within their department or team");
                }
                complaint.setAssignedTo(assignee);
                if (complaint.getStatus() == ComplaintStatus.OPEN) {
                    complaint.setStatus(ComplaintStatus.IN_PROGRESS);
                }
            }
            if (request.getResolution() != null) {
                complaint.setResolution(request.getResolution().trim());
            }
            if (request.getStatus() != null) {
                applyStatusTransition(complaint, request.getStatus());
            }
        }

        Complaint saved = complaintRepository.save(complaint);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ComplaintResponse assignComplaint(Long id, Long assignedToEmployeeId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot assign complaints");
        }

        if (assignedToEmployeeId == null) {
            throw new BadRequestException("Assignee employee ID is required");
        }

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
        if (currentUser.getRole() == Role.MANAGER) {
            validateManagerComplaintScope(complaint, currentEmp);
        }

        Employee assignee = employeeRepository.findById(assignedToEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", assignedToEmployeeId));

        if (assignee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BadRequestException("Cannot assign complaint to an inactive employee");
        }

        if (currentUser.getRole() == Role.MANAGER && !isWithinManagerScope(currentEmp, assignee)) {
            throw new AccessDeniedException("Access denied: Managers can only assign complaints to staff within their department or team");
        }

        complaint.setAssignedTo(assignee);
        if (complaint.getStatus() == ComplaintStatus.OPEN) {
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        }

        Complaint saved = complaintRepository.save(complaint);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ComplaintResponse updateComplaintStatus(Long id, ComplaintStatus status, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot update complaint status directly");
        }

        if (status == null) {
            throw new BadRequestException("Complaint status is required");
        }

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        if (currentUser.getRole() == Role.MANAGER) {
            Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
            validateManagerComplaintScope(complaint, currentEmp);
        }

        applyStatusTransition(complaint, status);

        Complaint saved = complaintRepository.save(complaint);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ComplaintResponse resolveComplaint(Long id, String resolution, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot resolve complaints");
        }

        if (!StringUtils.hasText(resolution)) {
            throw new BadRequestException("Resolution explanation is required");
        }

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", id));

        if (currentUser.getRole() == Role.MANAGER) {
            Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
            validateManagerComplaintScope(complaint, currentEmp);
        }

        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolution(resolution.trim());
        complaint.setResolvedAt(LocalDateTime.now());

        Complaint saved = complaintRepository.save(complaint);
        return mapToResponse(saved);
    }

    private void applyStatusTransition(Complaint complaint, ComplaintStatus newStatus) {
        if (complaint.getStatus() == newStatus) {
            return;
        }

        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        } else if (complaint.getStatus() == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(null);
        }

        complaint.setStatus(newStatus);
    }

    private void validateComplaintReadAccess(Complaint complaint, UserPrincipal currentUser) {
        Employee currentEmp = resolveCurrentEmployeeOrNull(currentUser);
        if (!hasComplaintReadPermission(complaint, currentUser, currentEmp)) {
            if (currentUser.getRole() == Role.EMPLOYEE) {
                throw new AccessDeniedException("Access denied: You can only view your own complaints");
            } else {
                throw new AccessDeniedException("Access denied: You do not have permission to view this complaint");
            }
        }
    }

    private boolean hasComplaintReadPermission(Complaint complaint, UserPrincipal currentUser, Employee currentEmp) {
        if (currentUser.getRole() == Role.ADMIN) {
            return true;
        }

        if (currentUser.getRole() == Role.EMPLOYEE) {
            return currentEmp != null && complaint.getEmployee().getId().equals(currentEmp.getId());
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (currentEmp == null) {
                return false;
            }
            boolean isOwner = complaint.getEmployee().getId().equals(currentEmp.getId());
            boolean isAssigned = complaint.getAssignedTo() != null && complaint.getAssignedTo().getId().equals(currentEmp.getId());
            boolean inScope = isWithinManagerScope(currentEmp, complaint.getEmployee());
            return isOwner || isAssigned || inScope;
        }

        return false;
    }

    private void validateManagerComplaintScope(Complaint complaint, Employee currentEmp) {
        if (currentEmp == null) {
            throw new AccessDeniedException("Manager employee record not found");
        }
        boolean isOwner = complaint.getEmployee().getId().equals(currentEmp.getId());
        boolean isAssigned = complaint.getAssignedTo() != null && complaint.getAssignedTo().getId().equals(currentEmp.getId());
        boolean inScope = isWithinManagerScope(currentEmp, complaint.getEmployee());

        if (!isOwner && !isAssigned && !inScope) {
            throw new AccessDeniedException("Access denied: You can only manage complaints within your department or team");
        }
    }

    private boolean isWithinManagerScope(Employee managerEmp, Employee targetEmp) {
        if (managerEmp.getId().equals(targetEmp.getId())) {
            return true;
        }
        boolean isDirectReport = targetEmp.getManager() != null && targetEmp.getManager().getId().equals(managerEmp.getId());
        boolean isSameDept = targetEmp.getDepartment() != null && managerEmp.getDepartment() != null
                && targetEmp.getDepartment().getId().equals(managerEmp.getDepartment().getId());
        return isDirectReport || isSameDept;
    }

    private Employee resolveCurrentEmployee(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile not found for user: " + currentUser.getUsername()));
    }

    private Employee resolveCurrentEmployeeOrNull(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElse(null);
    }

    private ComplaintResponse mapToResponse(Complaint c) {
        return ComplaintResponse.builder()
                .id(c.getId())
                .employeeId(c.getEmployee().getId())
                .employeeName(c.getEmployee().getFirstName() + " " + c.getEmployee().getLastName())
                .employeeCode(c.getEmployee().getEmployeeCode())
                .title(c.getTitle())
                .description(c.getDescription())
                .category(c.getCategory())
                .priority(c.getPriority())
                .status(c.getStatus())
                .assignedToId(c.getAssignedTo() != null ? c.getAssignedTo().getId() : null)
                .assignedToName(c.getAssignedTo() != null ? c.getAssignedTo().getFirstName() + " " + c.getAssignedTo().getLastName() : null)
                .resolution(c.getResolution())
                .resolvedAt(c.getResolvedAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}

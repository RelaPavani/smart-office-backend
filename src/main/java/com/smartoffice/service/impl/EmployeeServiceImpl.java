package com.smartoffice.service.impl;

import com.smartoffice.dto.employee.EmployeeCreateRequest;
import com.smartoffice.dto.employee.EmployeePersonalUpdateRequest;
import com.smartoffice.dto.employee.EmployeeResponse;
import com.smartoffice.dto.employee.EmployeeUpdateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.User;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.DepartmentRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.EmployeeService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        String code = request.getEmployeeCode().trim();
        if (employeeRepository.existsByEmployeeCode(code)) {
            throw new ConflictException("Employee code already exists: " + code);
        }

        String email = request.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmail(email)) {
            throw new ConflictException("Employee email already exists: " + email);
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        }

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", request.getManagerId()));
        }

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
            if (employeeRepository.existsByUserId(request.getUserId())) {
                throw new ConflictException("User account is already associated with an employee: " + request.getUserId());
            }
        }

        Employee employee = Employee.builder()
                .employeeCode(code)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .designation(request.getDesignation().trim())
                .joiningDate(request.getJoiningDate())
                .status(request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE)
                .department(department)
                .manager(manager)
                .user(user)
                .build();

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id, UserPrincipal currentUser) {
        validateEmployeeAccess(currentUser, id);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return mapToResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getCurrentEmployee(UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);
        return mapToResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse updatePersonalDetails(EmployeePersonalUpdateRequest request, UserPrincipal currentUser) {
        Employee employee = resolveCurrentEmployee(currentUser);

        if (StringUtils.hasText(request.getFirstName())) {
            employee.setFirstName(request.getFirstName().trim());
        }
        if (StringUtils.hasText(request.getLastName())) {
            employee.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone().trim());
        }

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees(
            Long departmentId,
            EmployeeStatus status,
            String search,
            UserPrincipal currentUser) {

        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Access denied: Employees cannot list all employees");
        }

        Long effectiveDeptId = departmentId;
        if (currentUser.getRole() == Role.MANAGER && departmentId == null) {
            Employee manager = employeeRepository.findByUserId(currentUser.getId())
                    .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                    .orElse(null);
            if (manager != null && manager.getDepartment() != null) {
                effectiveDeptId = manager.getDepartment().getId();
            }
        }

        Specification<Employee> spec = createSpecification(effectiveDeptId, status, search);
        return employeeRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployeesByDepartment(Long departmentId, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.EMPLOYEE) {
            Employee currentEmp = resolveCurrentEmployee(currentUser);
            if (currentEmp.getDepartment() == null || !currentEmp.getDepartment().getId().equals(departmentId)) {
                throw new AccessDeniedException("Access denied: You can only view employees in your own department");
            }
        } else if (currentUser.getRole() == Role.MANAGER) {
            Employee currentMgr = resolveCurrentEmployee(currentUser);
            if (currentMgr.getDepartment() != null && !currentMgr.getDepartment().getId().equals(departmentId)) {
                throw new AccessDeniedException("Access denied: You can only view employees in your managed department");
            }
        }

        return employeeRepository.findByDepartmentId(departmentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (StringUtils.hasText(request.getFirstName())) {
            employee.setFirstName(request.getFirstName().trim());
        }
        if (StringUtils.hasText(request.getLastName())) {
            employee.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone().trim());
        }
        if (StringUtils.hasText(request.getDesignation())) {
            employee.setDesignation(request.getDesignation().trim());
        }
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            employee.setDepartment(department);
        }
        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", request.getManagerId()));
            employee.setManager(manager);
        }

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateOrDeleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);
    }

    private void validateEmployeeAccess(UserPrincipal currentUser, Long targetEmployeeId) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        Employee currentEmp = resolveCurrentEmployee(currentUser);
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (!targetEmployeeId.equals(currentEmp.getId())) {
                throw new AccessDeniedException("Access denied: You can only view your own profile");
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
                throw new AccessDeniedException("Access denied: You can only view employees in your team or department");
            }
        }
    }

    public Employee resolveCurrentEmployee(UserPrincipal currentUser) {
        return employeeRepository.findByUserId(currentUser.getId())
                .or(() -> employeeRepository.findByEmail(currentUser.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee profile not found for user: " + currentUser.getUsername()));
    }

    private Specification<Employee> createSpecification(Long departmentId, EmployeeStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("firstName")), searchPattern),
                        cb.like(cb.lower(root.get("lastName")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(cb.lower(root.get("employeeCode")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private EmployeeResponse mapToResponse(Employee emp) {
        return EmployeeResponse.builder()
                .id(emp.getId())
                .employeeCode(emp.getEmployeeCode())
                .firstName(emp.getFirstName())
                .lastName(emp.getLastName())
                .email(emp.getEmail())
                .phone(emp.getPhone())
                .designation(emp.getDesignation())
                .joiningDate(emp.getJoiningDate())
                .status(emp.getStatus())
                .departmentId(emp.getDepartment() != null ? emp.getDepartment().getId() : null)
                .departmentName(emp.getDepartment() != null ? emp.getDepartment().getName() : null)
                .managerId(emp.getManager() != null ? emp.getManager().getId() : null)
                .managerName(emp.getManager() != null ? emp.getManager().getFirstName() + " " + emp.getManager().getLastName() : null)
                .userId(emp.getUser() != null ? emp.getUser().getId() : null)
                .createdAt(emp.getCreatedAt())
                .updatedAt(emp.getUpdatedAt())
                .build();
    }
}

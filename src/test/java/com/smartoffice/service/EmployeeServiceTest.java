package com.smartoffice.service;

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
import com.smartoffice.repository.DepartmentRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.repository.UserRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Employee employee1;
    private Employee employee2;
    private User user1;
    private UserPrincipal employeePrincipal;
    private UserPrincipal adminPrincipal;
    private Department department;

    @BeforeEach
    void setUp() {
        department = Department.builder()
                .id(1L)
                .name("Engineering")
                .build();

        user1 = User.builder()
                .id(101L)
                .username("emp1")
                .email("emp1@smartoffice.com")
                .role(Role.EMPLOYEE)
                .build();

        employee1 = Employee.builder()
                .id(1L)
                .employeeCode("EMP001")
                .firstName("John")
                .lastName("Doe")
                .email("emp1@smartoffice.com")
                .phone("1234567890")
                .designation("Software Engineer")
                .joiningDate(LocalDate.now().minusMonths(6))
                .status(EmployeeStatus.ACTIVE)
                .department(department)
                .user(user1)
                .build();

        employee2 = Employee.builder()
                .id(2L)
                .employeeCode("EMP002")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@smartoffice.com")
                .phone("9876543210")
                .designation("QA Engineer")
                .joiningDate(LocalDate.now().minusMonths(3))
                .status(EmployeeStatus.ACTIVE)
                .department(department)
                .build();

        employeePrincipal = new UserPrincipal(
                101L, "emp1", "emp1@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList()
        );

        adminPrincipal = new UserPrincipal(
                999L, "admin", "admin@smartoffice.com", "pass", Role.ADMIN, true, Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Create employee successfully")
    void testCreateEmployeeSuccess() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .employeeCode("EMP001")
                .firstName("John")
                .lastName("Doe")
                .email("emp1@smartoffice.com")
                .phone("1234567890")
                .designation("Software Engineer")
                .joiningDate(LocalDate.now().minusDays(1))
                .status(EmployeeStatus.ACTIVE)
                .departmentId(1L)
                .build();

        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("emp1@smartoffice.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee1);

        EmployeeResponse response = employeeService.createEmployee(request);

        assertNotNull(response);
        assertEquals("EMP001", response.getEmployeeCode());
        assertEquals("John", response.getFirstName());
    }

    @Test
    @DisplayName("Create employee with duplicate code throws ConflictException")
    void testCreateEmployeeDuplicateCodeThrowsConflict() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .employeeCode("EMP001")
                .email("new@smartoffice.com")
                .build();

        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    @DisplayName("Create employee with duplicate email throws ConflictException")
    void testCreateEmployeeDuplicateEmailThrowsConflict() {
        EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                .employeeCode("EMP999")
                .email("emp1@smartoffice.com")
                .build();

        when(employeeRepository.existsByEmployeeCode("EMP999")).thenReturn(false);
        when(employeeRepository.existsByEmail("emp1@smartoffice.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> employeeService.createEmployee(request));
    }

    @Test
    @DisplayName("Admin can view any employee by ID")
    void testAdminCanViewAnyEmployee() {
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee2));

        EmployeeResponse response = employeeService.getEmployeeById(2L, adminPrincipal);

        assertNotNull(response);
        assertEquals("EMP002", response.getEmployeeCode());
    }

    @Test
    @DisplayName("Employee can view own profile by ID")
    void testEmployeeCanViewOwnProfile() {
        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));

        EmployeeResponse response = employeeService.getEmployeeById(1L, employeePrincipal);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Employee cannot view another employee profile by ID (AccessDeniedException)")
    void testEmployeeCannotViewAnotherEmployee() {
        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));

        assertThrows(AccessDeniedException.class, () -> employeeService.getEmployeeById(2L, employeePrincipal));
    }

    @Test
    @DisplayName("Employee updates own allowed personal fields")
    void testEmployeeUpdatesPersonalDetails() {
        EmployeePersonalUpdateRequest request = EmployeePersonalUpdateRequest.builder()
                .firstName("Johnny")
                .lastName("Doe")
                .phone("555-0199")
                .build();

        when(employeeRepository.findByUserId(101L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        EmployeeResponse response = employeeService.updatePersonalDetails(request, employeePrincipal);

        assertNotNull(response);
        assertEquals("Johnny", response.getFirstName());
        assertEquals("555-0199", response.getPhone());
        // Verify protected fields like department, status, role remain intact
        assertEquals("Engineering", response.getDepartmentName());
        assertEquals(EmployeeStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Admin searches/filters employees successfully")
    void testAdminFiltersEmployees() {
        when(employeeRepository.findAll(any(Specification.class))).thenReturn(List.of(employee1, employee2));

        List<EmployeeResponse> list = employeeService.getAllEmployees(1L, EmployeeStatus.ACTIVE, "John", adminPrincipal);

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Employee cannot list all employees")
    void testEmployeeCannotListAllEmployees() {
        assertThrows(AccessDeniedException.class,
                () -> employeeService.getAllEmployees(null, null, null, employeePrincipal));
    }

    @Test
    @DisplayName("Admin deactivates employee successfully")
    void testAdminDeactivatesEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(i -> i.getArgument(0));

        employeeService.deactivateOrDeleteEmployee(1L);

        assertEquals(EmployeeStatus.INACTIVE, employee1.getStatus());
        verify(employeeRepository).save(employee1);
    }
}

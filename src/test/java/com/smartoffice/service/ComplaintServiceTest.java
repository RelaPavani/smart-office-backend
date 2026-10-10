package com.smartoffice.service;

import com.smartoffice.dto.complaint.ComplaintCreateRequest;
import com.smartoffice.dto.complaint.ComplaintResponse;
import com.smartoffice.dto.complaint.ComplaintUpdateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.Employee;
import com.smartoffice.entity.Complaint;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
import com.smartoffice.entity.enums.EmployeeStatus;
import com.smartoffice.entity.enums.Role;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.repository.ComplaintRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.impl.ComplaintServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal managerPrincipal;
    private UserPrincipal employeePrincipal;
    private UserPrincipal otherEmpPrincipal;

    private Department deptIT;
    private Department deptSales;
    private Employee managerEmp;
    private Employee complainantEmp;
    private Employee itStaffEmp;
    private Employee salesEmp;
    private Complaint testComplaint;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal(1L, "admin", "admin@smartoffice.com", "pass", Role.ADMIN, true, Collections.emptyList());
        managerPrincipal = new UserPrincipal(2L, "manager", "manager@smartoffice.com", "pass", Role.MANAGER, true, Collections.emptyList());
        employeePrincipal = new UserPrincipal(3L, "emp1", "emp1@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());
        otherEmpPrincipal = new UserPrincipal(4L, "emp2", "emp2@smartoffice.com", "pass", Role.EMPLOYEE, true, Collections.emptyList());

        deptIT = Department.builder().id(10L).name("IT").active(true).build();
        deptSales = Department.builder().id(20L).name("Sales").active(true).build();

        managerEmp = Employee.builder()
                .id(100L)
                .employeeCode("MGR001")
                .firstName("Alice")
                .lastName("Manager")
                .email("manager@smartoffice.com")
                .department(deptIT)
                .status(EmployeeStatus.ACTIVE)
                .build();

        complainantEmp = Employee.builder()
                .id(101L)
                .employeeCode("EMP001")
                .firstName("Bob")
                .lastName("Complainant")
                .email("emp1@smartoffice.com")
                .department(deptIT)
                .manager(managerEmp)
                .status(EmployeeStatus.ACTIVE)
                .build();

        itStaffEmp = Employee.builder()
                .id(102L)
                .employeeCode("EMP002")
                .firstName("Charlie")
                .lastName("Tech")
                .email("it@smartoffice.com")
                .department(deptIT)
                .manager(managerEmp)
                .status(EmployeeStatus.ACTIVE)
                .build();

        salesEmp = Employee.builder()
                .id(103L)
                .employeeCode("EMP003")
                .firstName("David")
                .lastName("Sales")
                .email("emp2@smartoffice.com")
                .department(deptSales)
                .status(EmployeeStatus.ACTIVE)
                .build();

        testComplaint = Complaint.builder()
                .id(50L)
                .employee(complainantEmp)
                .title("Monitor flickering")
                .description("HDMI port loose.")
                .category(ComplaintCategory.IT)
                .priority(ComplaintPriority.HIGH)
                .status(ComplaintStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("Employee creates own complaint successfully")
    void testEmployeeCreatesOwnComplaint() {
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .employeeId(101L)
                .title("Broken chair")
                .description("Chair wheel broken.")
                .category(ComplaintCategory.FACILITIES)
                .priority(ComplaintPriority.MEDIUM)
                .build();

        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(complainantEmp));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            c.setId(60L);
            return c;
        });

        ComplaintResponse response = complaintService.createComplaint(request, employeePrincipal);

        assertNotNull(response);
        assertEquals(60L, response.getId());
        assertEquals(ComplaintStatus.OPEN, response.getStatus());
        assertEquals(101L, response.getEmployeeId());
    }

    @Test
    @DisplayName("Employee cannot create complaint on behalf of another employee")
    void testEmployeeCannotCreateForOtherEmployeeThrows() {
        ComplaintCreateRequest request = ComplaintCreateRequest.builder()
                .employeeId(103L)
                .title("Another person's issue")
                .description("Description")
                .category(ComplaintCategory.IT)
                .priority(ComplaintPriority.LOW)
                .build();

        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(complainantEmp));

        assertThrows(AccessDeniedException.class, () -> complaintService.createComplaint(request, employeePrincipal));
    }

    @Test
    @DisplayName("Employee can read own complaint")
    void testEmployeeCanReadOwnComplaint() {
        when(complaintRepository.findById(50L)).thenReturn(Optional.of(testComplaint));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(complainantEmp));

        ComplaintResponse response = complaintService.getComplaintById(50L, employeePrincipal);

        assertNotNull(response);
        assertEquals(50L, response.getId());
    }

    @Test
    @DisplayName("Employee cannot read another employee's complaint")
    void testEmployeeCannotReadOtherComplaintThrows() {
        when(complaintRepository.findById(50L)).thenReturn(Optional.of(testComplaint));
        when(employeeRepository.findByUserId(4L)).thenReturn(Optional.of(salesEmp));

        assertThrows(AccessDeniedException.class, () -> complaintService.getComplaintById(50L, otherEmpPrincipal));
    }

    @Test
    @DisplayName("Manager assigns complaint to team member")
    void testManagerAssignsComplaint() {
        when(complaintRepository.findById(50L)).thenReturn(Optional.of(testComplaint));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(managerEmp));
        when(employeeRepository.findById(102L)).thenReturn(Optional.of(itStaffEmp));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.assignComplaint(50L, 102L, managerPrincipal);

        assertEquals(ComplaintStatus.IN_PROGRESS, response.getStatus());
        assertEquals(102L, response.getAssignedToId());
    }

    @Test
    @DisplayName("Manager cannot assign complaint to employee outside scope")
    void testManagerAssignsOutsideScopeThrows() {
        when(complaintRepository.findById(50L)).thenReturn(Optional.of(testComplaint));
        when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(managerEmp));
        when(employeeRepository.findById(103L)).thenReturn(Optional.of(salesEmp));

        assertThrows(AccessDeniedException.class, () -> complaintService.assignComplaint(50L, 103L, managerPrincipal));
    }

    @Test
    @DisplayName("Admin or Manager resolves complaint with explanation")
    void testResolveComplaintSuccess() {
        testComplaint.setStatus(ComplaintStatus.IN_PROGRESS);
        testComplaint.setAssignedTo(itStaffEmp);

        when(complaintRepository.findById(50L)).thenReturn(Optional.of(testComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplaintResponse response = complaintService.resolveComplaint(50L, "Cable replaced and tested.", adminPrincipal);

        assertEquals(ComplaintStatus.RESOLVED, response.getStatus());
        assertEquals("Cable replaced and tested.", response.getResolution());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    @DisplayName("Employee cannot resolve complaint")
    void testEmployeeCannotResolveThrows() {
        assertThrows(AccessDeniedException.class, () -> complaintService.resolveComplaint(50L, "Done myself", employeePrincipal));
    }

    @Test
    @DisplayName("Employee cannot update complaint if already in progress")
    void testEmployeeCannotUpdateInProgressComplaintThrows() {
        testComplaint.setStatus(ComplaintStatus.IN_PROGRESS);

        when(complaintRepository.findById(50L)).thenReturn(Optional.of(testComplaint));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(complainantEmp));

        ComplaintUpdateRequest updateRequest = ComplaintUpdateRequest.builder()
                .category(ComplaintCategory.OTHER)
                .build();

        assertThrows(BadRequestException.class, () -> complaintService.updateComplaint(50L, updateRequest, employeePrincipal));
    }
}

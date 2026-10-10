package com.smartoffice.service;

import com.smartoffice.dto.department.DepartmentCreateRequest;
import com.smartoffice.dto.department.DepartmentResponse;
import com.smartoffice.dto.department.DepartmentUpdateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.DepartmentRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department sampleDepartment;

    @BeforeEach
    void setUp() {
        sampleDepartment = Department.builder()
                .id(1L)
                .name("Engineering")
                .description("Software engineering team")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Create department successfully")
    void testCreateDepartmentSuccess() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("Engineering")
                .description("Software engineering team")
                .active(true)
                .build();

        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(sampleDepartment);

        DepartmentResponse response = departmentService.createDepartment(request);

        assertNotNull(response);
        assertEquals("Engineering", response.getName());
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("Create department with duplicate name throws ConflictException")
    void testCreateDepartmentDuplicateThrowsConflict() {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .name("Engineering")
                .build();

        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

        assertThrows(ConflictException.class, () -> departmentService.createDepartment(request));
    }

    @Test
    @DisplayName("Get department by ID successfully")
    void testGetDepartmentByIdSuccess() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));

        DepartmentResponse response = departmentService.getDepartmentById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Engineering", response.getName());
    }

    @Test
    @DisplayName("Get non-existent department throws ResourceNotFoundException")
    void testGetDepartmentNotFoundThrowsException() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }

    @Test
    @DisplayName("Update department successfully")
    void testUpdateDepartmentSuccess() {
        DepartmentUpdateRequest request = DepartmentUpdateRequest.builder()
                .name("R&D")
                .description("Research and Development")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("R&D", 1L)).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(i -> i.getArgument(0));

        DepartmentResponse response = departmentService.updateDepartment(1L, request);

        assertNotNull(response);
        assertEquals("R&D", response.getName());
        assertEquals("Research and Development", response.getDescription());
    }

    @Test
    @DisplayName("Update department with duplicate name throws ConflictException")
    void testUpdateDepartmentDuplicateNameThrowsConflict() {
        DepartmentUpdateRequest request = DepartmentUpdateRequest.builder()
                .name("HR")
                .build();

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("HR", 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> departmentService.updateDepartment(1L, request));
    }

    @Test
    @DisplayName("Delete department successfully when no employees are assigned")
    void testDeleteDepartmentSuccess() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).delete(sampleDepartment);
    }

    @Test
    @DisplayName("Delete department with active employees throws BadRequestException")
    void testDeleteDepartmentWithEmployeesThrowsBadRequest() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> departmentService.deleteDepartment(1L));
    }

    @Test
    @DisplayName("Search departments by name returns matching list")
    void testSearchDepartmentsByName() {
        when(departmentRepository.findByNameContainingIgnoreCase("eng"))
                .thenReturn(List.of(sampleDepartment));

        List<DepartmentResponse> list = departmentService.getAllDepartments("eng");

        assertEquals(1, list.size());
        assertEquals("Engineering", list.get(0).getName());
    }
}

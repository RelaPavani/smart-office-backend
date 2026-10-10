package com.smartoffice.service.impl;

import com.smartoffice.dto.department.DepartmentCreateRequest;
import com.smartoffice.dto.department.DepartmentResponse;
import com.smartoffice.dto.department.DepartmentUpdateRequest;
import com.smartoffice.entity.Department;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.exception.ConflictException;
import com.smartoffice.exception.ResourceNotFoundException;
import com.smartoffice.repository.DepartmentRepository;
import com.smartoffice.repository.EmployeeRepository;
import com.smartoffice.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        String deptName = request.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(deptName)) {
            throw new ConflictException("Department with name '" + deptName + "' already exists");
        }

        Department department = Department.builder()
                .name(deptName)
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Department saved = departmentRepository.save(department);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
        return mapToResponse(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments(String search) {
        List<Department> departments;
        if (StringUtils.hasText(search)) {
            departments = departmentRepository.findByNameContainingIgnoreCase(search.trim());
        } else {
            departments = departmentRepository.findAll();
        }
        return departments.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (StringUtils.hasText(request.getName())) {
            String newName = request.getName().trim();
            if (departmentRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
                throw new ConflictException("Department with name '" + newName + "' already exists");
            }
            department.setName(newName);
        }

        if (request.getDescription() != null) {
            department.setDescription(request.getDescription().trim());
        }

        if (request.getActive() != null) {
            department.setActive(request.getActive());
        }

        Department updated = departmentRepository.save(department);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));

        if (employeeRepository.existsByDepartmentId(id)) {
            throw new BadRequestException("Cannot delete department with active employees assigned");
        }

        departmentRepository.delete(department);
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .active(department.isActive())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}

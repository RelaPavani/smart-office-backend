package com.smartoffice.service;

import com.smartoffice.dto.department.DepartmentCreateRequest;
import com.smartoffice.dto.department.DepartmentResponse;
import com.smartoffice.dto.department.DepartmentUpdateRequest;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentCreateRequest request);

    DepartmentResponse getDepartmentById(Long id);

    List<DepartmentResponse> getAllDepartments(String search);

    DepartmentResponse updateDepartment(Long id, DepartmentUpdateRequest request);

    void deleteDepartment(Long id);
}

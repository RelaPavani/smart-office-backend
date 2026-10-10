package com.smartoffice.repository;

import com.smartoffice.entity.Employee;
import com.smartoffice.entity.enums.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCodeIgnoreCaseAndIdNot(String employeeCode, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Employee> findByDepartmentId(Long departmentId);

    boolean existsByDepartmentId(Long departmentId);

    List<Employee> findByDepartmentIdAndStatus(Long departmentId, EmployeeStatus status);

    List<Employee> findByManagerId(Long managerId);

    List<Employee> findByStatus(EmployeeStatus status);

    Optional<Employee> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndIdNot(Long userId, Long id);
}

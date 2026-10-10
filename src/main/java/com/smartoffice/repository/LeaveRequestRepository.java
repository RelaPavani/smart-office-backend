package com.smartoffice.repository;

import com.smartoffice.entity.LeaveRequest;
import com.smartoffice.entity.enums.LeaveStatus;
import com.smartoffice.entity.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeId(Long employeeId);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    List<LeaveRequest> findByLeaveType(LeaveType leaveType);

    List<LeaveRequest> findByStartDateBetween(LocalDate startDate, LocalDate endDate);

    List<LeaveRequest> findByEmployeeDepartmentId(Long departmentId);

    List<LeaveRequest> findByEmployeeDepartmentIdAndStatus(Long departmentId, LeaveStatus status);

    List<LeaveRequest> findByEmployeeDepartmentIdAndStartDateBetween(Long departmentId, LocalDate startDate, LocalDate endDate);

    List<LeaveRequest> findByEmployeeIdAndStartDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId AND lr.status IN :statuses AND lr.startDate <= :endDate AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlappingLeaves(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveStatus> statuses
    );
}

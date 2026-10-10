package com.smartoffice.repository;

import com.smartoffice.entity.LeaveBalance;
import com.smartoffice.entity.enums.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndYearAndLeaveType(Long employeeId, Integer year, LeaveType leaveType);

    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);

    List<LeaveBalance> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndYearAndLeaveType(Long employeeId, Integer year, LeaveType leaveType);
}

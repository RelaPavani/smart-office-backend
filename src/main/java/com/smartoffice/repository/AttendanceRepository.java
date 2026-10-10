package com.smartoffice.repository;

import com.smartoffice.entity.Attendance;
import com.smartoffice.entity.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployeeId(Long employeeId);

    List<Attendance> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByAttendanceDate(LocalDate attendanceDate);

    List<Attendance> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<Attendance> findByAttendanceDateAndStatus(LocalDate attendanceDate, AttendanceStatus status);

    List<Attendance> findByEmployeeDepartmentIdAndAttendanceDate(Long departmentId, LocalDate attendanceDate);

    List<Attendance> findByEmployeeDepartmentIdAndAttendanceDateBetween(Long departmentId, LocalDate startDate, LocalDate endDate);

    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

    List<Attendance> findByEmployeeDepartmentId(Long departmentId);
}

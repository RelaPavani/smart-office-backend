package com.smartoffice.repository;

import com.smartoffice.entity.Complaint;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByEmployeeId(Long employeeId);

    List<Complaint> findByEmployeeIdAndStatus(Long employeeId, ComplaintStatus status);

    List<Complaint> findByAssignedToId(Long employeeId);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findByCategory(ComplaintCategory category);

    List<Complaint> findByPriority(ComplaintPriority priority);

    List<Complaint> findByEmployeeDepartmentId(Long departmentId);
}

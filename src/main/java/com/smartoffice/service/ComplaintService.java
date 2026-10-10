package com.smartoffice.service;

import com.smartoffice.dto.complaint.ComplaintCreateRequest;
import com.smartoffice.dto.complaint.ComplaintResponse;
import com.smartoffice.dto.complaint.ComplaintUpdateRequest;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
import com.smartoffice.security.UserPrincipal;

import java.util.List;

public interface ComplaintService {

    ComplaintResponse createComplaint(ComplaintCreateRequest request, UserPrincipal currentUser);

    ComplaintResponse getComplaintById(Long id, UserPrincipal currentUser);

    List<ComplaintResponse> listComplaints(ComplaintStatus status, ComplaintCategory category, ComplaintPriority priority, Long employeeId, UserPrincipal currentUser);

    List<ComplaintResponse> getMyComplaints(UserPrincipal currentUser);

    ComplaintResponse updateComplaint(Long id, ComplaintUpdateRequest request, UserPrincipal currentUser);

    ComplaintResponse assignComplaint(Long id, Long assignedToEmployeeId, UserPrincipal currentUser);

    ComplaintResponse updateComplaintStatus(Long id, ComplaintStatus status, UserPrincipal currentUser);

    ComplaintResponse resolveComplaint(Long id, String resolution, UserPrincipal currentUser);
}

package com.smartoffice.service;

import com.smartoffice.dto.leave.LeaveActionRequest;
import com.smartoffice.dto.leave.LeaveBalanceCreateRequest;
import com.smartoffice.dto.leave.LeaveBalanceResponse;
import com.smartoffice.dto.leave.LeaveBalanceUpdateRequest;
import com.smartoffice.dto.leave.LeaveRequestCreateRequest;
import com.smartoffice.dto.leave.LeaveRequestResponse;
import com.smartoffice.security.UserPrincipal;

import java.util.List;

public interface LeaveService {

    LeaveRequestResponse applyForLeave(LeaveRequestCreateRequest request, UserPrincipal currentUser);

    List<LeaveRequestResponse> getMyLeaves(UserPrincipal currentUser);

    LeaveRequestResponse getLeaveById(Long id, UserPrincipal currentUser);

    List<LeaveRequestResponse> getPendingLeaves(UserPrincipal currentUser);

    List<LeaveRequestResponse> getLeavesByEmployee(Long employeeId, UserPrincipal currentUser);

    LeaveRequestResponse approveLeave(Long id, LeaveActionRequest actionRequest, UserPrincipal currentUser);

    LeaveRequestResponse rejectLeave(Long id, LeaveActionRequest actionRequest, UserPrincipal currentUser);

    List<LeaveBalanceResponse> getMyLeaveBalances(Integer year, UserPrincipal currentUser);

    List<LeaveBalanceResponse> getLeaveBalancesByEmployee(Long employeeId, Integer year, UserPrincipal currentUser);

    LeaveBalanceResponse createLeaveBalance(LeaveBalanceCreateRequest request);

    LeaveBalanceResponse updateLeaveBalance(Long id, LeaveBalanceUpdateRequest request);
}

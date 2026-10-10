package com.smartoffice.controller;

import com.smartoffice.dto.complaint.ComplaintCreateRequest;
import com.smartoffice.dto.complaint.ComplaintResponse;
import com.smartoffice.dto.complaint.ComplaintUpdateRequest;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.ComplaintStatus;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> createComplaint(
            @Valid @RequestBody ComplaintCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.createComplaint(request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Complaint submitted successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> listComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(required = false) ComplaintPriority priority,
            @RequestParam(required = false) Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ComplaintResponse> list = complaintService.listComplaints(status, category, priority, employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaints retrieved successfully", list));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> getMyComplaints(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<ComplaintResponse> list = complaintService.getMyComplaints(currentUser);
        return ResponseEntity.ok(ApiResponse.success("My complaints retrieved successfully", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> getComplaintById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.getComplaintById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaint retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintResponse response = complaintService.updateComplaint(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaint updated successfully", response));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> assignComplaint(
            @PathVariable Long id,
            @RequestParam(required = false) Long assignedToId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long targetAssigneeId = assignedToId;
        if (targetAssigneeId == null && body != null && body.containsKey("assignedToId")) {
            targetAssigneeId = Long.valueOf(body.get("assignedToId").toString());
        }
        if (targetAssigneeId == null) {
            throw new BadRequestException("Assignee employee ID is required");
        }
        ComplaintResponse response = complaintService.assignComplaint(id, targetAssigneeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaint assigned successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ComplaintStatus finalStatus = status;
        if (finalStatus == null && body != null && body.containsKey("status")) {
            try {
                finalStatus = ComplaintStatus.valueOf(body.get("status").toString().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid complaint status: " + body.get("status"));
            }
        }
        if (finalStatus == null) {
            throw new BadRequestException("Complaint status is required");
        }
        ComplaintResponse response = complaintService.updateComplaintStatus(id, finalStatus, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaint status updated successfully", response));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ComplaintResponse>> resolveComplaint(
            @PathVariable Long id,
            @RequestParam(required = false) String resolution,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        String finalResolution = resolution;
        if ((finalResolution == null || finalResolution.isBlank()) && body != null && body.containsKey("resolution")) {
            finalResolution = body.get("resolution").toString();
        }
        if (finalResolution == null || finalResolution.isBlank()) {
            throw new BadRequestException("Resolution explanation is required");
        }
        ComplaintResponse response = complaintService.resolveComplaint(id, finalResolution, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Complaint resolved successfully", response));
    }
}

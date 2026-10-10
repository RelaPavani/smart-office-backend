package com.smartoffice.controller;

import com.smartoffice.dto.meeting.MeetingCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantCreateRequest;
import com.smartoffice.dto.meeting.MeetingParticipantResponse;
import com.smartoffice.dto.meeting.MeetingParticipantUpdateRequest;
import com.smartoffice.dto.meeting.MeetingResponse;
import com.smartoffice.dto.meeting.MeetingUpdateRequest;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.entity.enums.MeetingStatus;
import com.smartoffice.entity.enums.ParticipantStatus;
import com.smartoffice.exception.BadRequestException;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @Valid @RequestBody MeetingCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MeetingResponse response = meetingService.createMeeting(request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Meeting scheduled successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> listMeetings(
            @RequestParam(required = false) MeetingStatus status,
            @RequestParam(required = false) Long organizerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<MeetingResponse> list = meetingService.listMeetings(status, organizerId, from, to, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Meetings retrieved successfully", list));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMyMeetings(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<MeetingResponse> list = meetingService.getMyMeetings(currentUser);
        return ResponseEntity.ok(ApiResponse.success("My meetings retrieved successfully", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeetingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MeetingResponse response = meetingService.getMeetingById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Meeting retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
            @PathVariable Long id,
            @Valid @RequestBody MeetingUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MeetingResponse response = meetingService.updateMeeting(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Meeting updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeetingStatus(
            @PathVariable Long id,
            @RequestParam(required = false) MeetingStatus status,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MeetingStatus finalStatus = status;
        if (finalStatus == null && body != null && body.containsKey("status")) {
            try {
                finalStatus = MeetingStatus.valueOf(body.get("status").toString().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid meeting status: " + body.get("status"));
            }
        }
        if (finalStatus == null) {
            throw new BadRequestException("Meeting status is required");
        }
        MeetingResponse response = meetingService.updateMeetingStatus(id, finalStatus, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Meeting status updated successfully", response));
    }

    @PostMapping("/{id}/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<MeetingParticipantResponse>> addParticipant(
            @PathVariable Long id,
            @Valid @RequestBody MeetingParticipantCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        request.setMeetingId(id);
        MeetingParticipantResponse response = meetingService.addParticipant(id, request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Participant added successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<MeetingParticipantResponse>>> getMeetingParticipants(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<MeetingParticipantResponse> list = meetingService.getMeetingParticipants(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Meeting participants retrieved successfully", list));
    }

    @DeleteMapping("/{id}/participants/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @PathVariable Long id,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        meetingService.removeParticipant(id, employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Participant removed successfully", null));
    }

    @PatchMapping("/{id}/participants/{employeeId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<MeetingParticipantResponse>> updateParticipantStatus(
            @PathVariable Long id,
            @PathVariable Long employeeId,
            @RequestParam(required = false) ParticipantStatus status,
            @RequestBody(required = false) MeetingParticipantUpdateRequest requestBody,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ParticipantStatus finalStatus = status;
        if (finalStatus == null && requestBody != null && requestBody.getAttendanceStatus() != null) {
            finalStatus = requestBody.getAttendanceStatus();
        }
        if (finalStatus == null) {
            throw new BadRequestException("Attendance status is required");
        }
        MeetingParticipantResponse response = meetingService.updateParticipantStatus(id, employeeId, finalStatus, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Participant attendance status updated successfully", response));
    }
}

package com.smartoffice.controller;

import com.smartoffice.dto.announcement.AnnouncementCreateRequest;
import com.smartoffice.dto.announcement.AnnouncementResponse;
import com.smartoffice.dto.announcement.AnnouncementUpdateRequest;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.AnnouncementService;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @Valid @RequestBody AnnouncementCreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AnnouncementResponse response = announcementService.createAnnouncement(request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Announcement published successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> listAnnouncements(
            @RequestParam(required = false) Boolean active,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<AnnouncementResponse> list = announcementService.listAnnouncements(active, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved successfully", list));
    }

    @GetMapping("/published")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getPublishedAnnouncements() {
        List<AnnouncementResponse> list = announcementService.getPublishedAnnouncements();
        return ResponseEntity.ok(ApiResponse.success("Published announcements feed retrieved successfully", list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AnnouncementResponse response = announcementService.getAnnouncementById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AnnouncementResponse response = announcementService.updateAnnouncement(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement updated successfully", response));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> togglePublish(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Boolean finalActive = active;
        if (finalActive == null && body != null && body.containsKey("active")) {
            finalActive = Boolean.valueOf(body.get("active").toString());
        }
        AnnouncementResponse response = announcementService.togglePublish(id, finalActive, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement publication status updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        announcementService.deleteAnnouncement(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully", null));
    }
}

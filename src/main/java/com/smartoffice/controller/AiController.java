package com.smartoffice.controller;

import com.smartoffice.dto.ai.AssistantChatRequest;
import com.smartoffice.dto.ai.AssistantChatResponse;
import com.smartoffice.dto.ai.ComplaintClassificationRequest;
import com.smartoffice.dto.ai.ComplaintClassificationResponse;
import com.smartoffice.dto.ai.EmployeePerformanceSummaryResponse;
import com.smartoffice.dto.ai.MeetingNotesSummaryRequest;
import com.smartoffice.dto.ai.MeetingNotesSummaryResponse;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionRequest;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionResponse;
import com.smartoffice.dto.response.ApiResponse;
import com.smartoffice.security.UserPrincipal;
import com.smartoffice.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/complaints/classify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ComplaintClassificationResponse>> classifyComplaint(
            @Valid @RequestBody ComplaintClassificationRequest request) {
        ComplaintClassificationResponse response = aiService.classifyComplaint(request);
        return ResponseEntity.ok(ApiResponse.success("Complaint classified successfully", response));
    }

    @PostMapping("/tasks/suggest-subtasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TaskSubtaskSuggestionResponse>> suggestSubtasks(
            @Valid @RequestBody TaskSubtaskSuggestionRequest request) {
        TaskSubtaskSuggestionResponse response = aiService.suggestSubtasks(request);
        return ResponseEntity.ok(ApiResponse.success("Task subtasks suggested successfully", response));
    }

    @GetMapping("/employees/{employeeId}/performance-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeePerformanceSummaryResponse>> getPerformanceSummary(
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        EmployeePerformanceSummaryResponse response = aiService.getEmployeePerformanceSummary(employeeId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Employee performance summary retrieved successfully", response));
    }

    @PostMapping("/meetings/summarize")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<MeetingNotesSummaryResponse>> summarizeMeeting(
            @Valid @RequestBody MeetingNotesSummaryRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        MeetingNotesSummaryResponse response = aiService.summarizeMeetingNotes(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Meeting notes summarized successfully", response));
    }

    @PostMapping("/assistant/chat")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<AssistantChatResponse>> chatAssistant(
            @Valid @RequestBody AssistantChatRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AssistantChatResponse response = aiService.chatAssistant(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Assistant responded successfully", response));
    }
}

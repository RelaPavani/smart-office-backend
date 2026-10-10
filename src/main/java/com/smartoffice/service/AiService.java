package com.smartoffice.service;

import com.smartoffice.dto.ai.AssistantChatRequest;
import com.smartoffice.dto.ai.AssistantChatResponse;
import com.smartoffice.dto.ai.ComplaintClassificationRequest;
import com.smartoffice.dto.ai.ComplaintClassificationResponse;
import com.smartoffice.dto.ai.EmployeePerformanceSummaryResponse;
import com.smartoffice.dto.ai.MeetingNotesSummaryRequest;
import com.smartoffice.dto.ai.MeetingNotesSummaryResponse;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionRequest;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionResponse;
import com.smartoffice.security.UserPrincipal;

public interface AiService {

    ComplaintClassificationResponse classifyComplaint(ComplaintClassificationRequest request);

    TaskSubtaskSuggestionResponse suggestSubtasks(TaskSubtaskSuggestionRequest request);

    EmployeePerformanceSummaryResponse getEmployeePerformanceSummary(Long employeeId, UserPrincipal currentUser);

    MeetingNotesSummaryResponse summarizeMeetingNotes(MeetingNotesSummaryRequest request, UserPrincipal currentUser);

    AssistantChatResponse chatAssistant(AssistantChatRequest request, UserPrincipal currentUser);
}

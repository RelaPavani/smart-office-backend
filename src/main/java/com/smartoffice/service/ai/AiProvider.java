package com.smartoffice.service.ai;

import com.smartoffice.dto.ai.ComplaintClassificationResponse;
import com.smartoffice.dto.ai.MeetingNotesSummaryResponse;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionResponse;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.enums.TaskPriority;

import java.time.LocalDateTime;
import java.util.List;

public interface AiProvider {

    boolean isAvailable();

    String getProviderName();

    ComplaintClassificationResponse classifyComplaint(String title, String description, List<Department> departments);

    TaskSubtaskSuggestionResponse suggestSubtasks(String taskTitle, String taskDescription, TaskPriority priority, LocalDateTime deadline);

    MeetingNotesSummaryResponse summarizeMeeting(Long meetingId, String notes);

    String generateOfficeAssistantAnswer(String question, String userContext);
}

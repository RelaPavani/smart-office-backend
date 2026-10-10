package com.smartoffice.service.ai;

import com.smartoffice.dto.ai.ComplaintClassificationResponse;
import com.smartoffice.dto.ai.MeetingNotesSummaryResponse;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionResponse;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.TaskPriority;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class FallbackAiProvider implements AiProvider {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "FALLBACK";
    }

    @Override
    public ComplaintClassificationResponse classifyComplaint(String title, String description, List<Department> departments) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ROOT);

        ComplaintPriority priority = ComplaintPriority.LOW;
        if (containsAny(combined, "urgent", "immediately", "asap", "emergency", "critical", "disaster", "blocker", "fire", "security breach", "severe")) {
            priority = ComplaintPriority.URGENT;
        } else if (containsAny(combined, "broken", "high", "error", "failed", "crash", "major", "down", "not working", "cannot work", "unable to")) {
            priority = ComplaintPriority.HIGH;
        } else if (containsAny(combined, "delay", "slow", "issue", "problem", "glitch", "medium", "help")) {
            priority = ComplaintPriority.MEDIUM;
        }

        ComplaintCategory category = ComplaintCategory.GENERAL;
        if (containsAny(combined, "salary", "pay", "payroll", "payslip", "bonus", "tax", "compensation", "wage", "deduction", "reimbursement")) {
            category = ComplaintCategory.PAYROLL;
        } else if (containsAny(combined, "wifi", "internet", "network", "laptop", "computer", "pc", "monitor", "keyboard", "mouse", "printer", "server", "vpn", "software", "password", "login", "bug", "email")) {
            category = ComplaintCategory.IT;
        } else if (containsAny(combined, "chair", "desk", "ac", "air condition", "air conditioning", "temperature", "heating", "light", "bulb", "cleaning", "cafeteria", "coffee", "water", "washroom", "toilet", "bathroom", "restroom", "door", "elevator", "parking", "leak")) {
            category = ComplaintCategory.FACILITIES;
        } else if (containsAny(combined, "harassment", "leave", "holiday", "policy", "behavior", "boss", "discrimination", "manager", "benefits", "contract", "human resource", "hr")) {
            category = ComplaintCategory.HR;
        }

        String suggestedDeptName = null;
        Long suggestedDeptId = null;

        if (departments != null && !departments.isEmpty()) {
            final ComplaintCategory targetCategory = category;
            Department matched = departments.stream()
                    .filter(d -> matchesDepartment(d.getName(), targetCategory))
                    .findFirst()
                    .orElse(null);

            if (matched != null) {
                suggestedDeptName = matched.getName();
                suggestedDeptId = matched.getId();
            }
        }

        return ComplaintClassificationResponse.builder()
                .suggestedCategory(category)
                .suggestedPriority(priority)
                .suggestedDepartment(suggestedDeptName)
                .suggestedDepartmentId(suggestedDeptId)
                .explanation("Classified as " + category + " with " + priority + " priority based on keyword analysis.")
                .confidence(0.85)
                .mode("FALLBACK")
                .build();
    }

    @Override
    public TaskSubtaskSuggestionResponse suggestSubtasks(String taskTitle, String taskDescription, TaskPriority priority, LocalDateTime deadline) {
        List<String> subtasks = new ArrayList<>();
        subtasks.add("Clarify scope, objectives, and prerequisites for: " + (taskTitle != null ? taskTitle : "task"));
        subtasks.add("Set up required tools, dependencies, and environment");
        subtasks.add("Execute core implementation and deliverables");
        subtasks.add("Conduct thorough review, testing, and edge-case verification");
        if (deadline != null) {
            subtasks.add("Prepare final documentation and complete handover before " + deadline.toLocalDate());
        } else {
            subtasks.add("Document completed work and notify stakeholders");
        }

        return TaskSubtaskSuggestionResponse.builder()
                .taskTitle(taskTitle)
                .suggestedSubtasks(subtasks)
                .explanation("Structured procedural subtasks suggested based on task objectives.")
                .mode("FALLBACK")
                .build();
    }

    @Override
    public MeetingNotesSummaryResponse summarizeMeeting(Long meetingId, String notes) {
        List<String> keyPoints = new ArrayList<>();
        List<String> actionItems = new ArrayList<>();

        if (notes != null && !notes.isBlank()) {
            String[] lines = notes.split("\\r?\\n");
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.startsWith("todo:") || lower.startsWith("action:") || lower.contains("action item") ||
                        lower.contains("assigned to") || lower.contains("will implement") || lower.contains("follow up")) {
                    actionItems.add(cleanItem(line));
                } else if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•") || line.matches("^\\d+\\..*")) {
                    keyPoints.add(cleanItem(line));
                } else {
                    keyPoints.add(line);
                }
            }
        }

        if (keyPoints.isEmpty()) {
            keyPoints.add("Discussed meeting topics and reviewed ongoing progress.");
        }
        if (actionItems.isEmpty()) {
            actionItems.add("Follow up on agenda points and review deliverables discussed during the meeting.");
        }

        String summary = "Meeting summary: Reviewed agenda topics with " + keyPoints.size() +
                " key point(s) documented and " + actionItems.size() + " actionable follow-up task(s) identified.";

        return MeetingNotesSummaryResponse.builder()
                .meetingId(meetingId)
                .summary(summary)
                .keyPoints(keyPoints)
                .actionItems(actionItems)
                .mode("FALLBACK")
                .build();
    }

    @Override
    public String generateOfficeAssistantAnswer(String question, String userContext) {
        return "Assistant (Deterministic Mode):\n" +
                (userContext != null && !userContext.isBlank() ? userContext : "I am here to assist with your Smart Office tasks, attendance, leaves, and meetings.");
    }

    private boolean containsAny(String text, String... keywords) {
        return Arrays.stream(keywords).anyMatch(text::contains);
    }

    private boolean matchesDepartment(String deptName, ComplaintCategory category) {
        if (deptName == null) {
            return false;
        }
        String lower = deptName.toLowerCase(Locale.ROOT);
        return switch (category) {
            case IT -> lower.contains("it") || lower.contains("tech") || lower.contains("information") || lower.contains("software");
            case HR -> lower.contains("hr") || lower.contains("human") || lower.contains("people");
            case FACILITIES -> lower.contains("facilities") || lower.contains("admin") || lower.contains("maintenance") || lower.contains("ops") || lower.contains("operations");
            case PAYROLL -> lower.contains("payroll") || lower.contains("finance") || lower.contains("account");
            default -> false;
        };
    }

    private String cleanItem(String line) {
        return line.replaceFirst("^[-*•\\d.]+\\s*", "").replaceFirst("^(?i)(action:|todo:)\\s*", "").trim();
    }
}

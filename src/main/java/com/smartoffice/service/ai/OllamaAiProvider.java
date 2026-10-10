package com.smartoffice.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartoffice.dto.ai.ComplaintClassificationResponse;
import com.smartoffice.dto.ai.MeetingNotesSummaryResponse;
import com.smartoffice.dto.ai.TaskSubtaskSuggestionResponse;
import com.smartoffice.entity.Department;
import com.smartoffice.entity.enums.ComplaintCategory;
import com.smartoffice.entity.enums.ComplaintPriority;
import com.smartoffice.entity.enums.TaskPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OllamaAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiProvider.class);

    private final String baseUrl;
    private final String model;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final FallbackAiProvider fallbackProvider;

    public OllamaAiProvider(
            @Value("${ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ai.ollama.model:llama3.2}") String model,
            @Value("${ai.ollama.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${ai.ollama.read-timeout-ms:8000}") int readTimeoutMs,
            ObjectMapper objectMapper,
            FallbackAiProvider fallbackProvider) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.objectMapper = objectMapper;
        this.fallbackProvider = fallbackProvider;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofMillis(connectTimeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Ollama is not available at {}: {}", baseUrl, e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "OLLAMA";
    }

    @Override
    public ComplaintClassificationResponse classifyComplaint(String title, String description, List<Department> departments) {
        if (!isAvailable()) {
            return fallbackProvider.classifyComplaint(title, description, departments);
        }

        try {
            String systemPrompt = "You are a corporate smart office assistant. Analyze the complaint and return a strictly formatted JSON object with: " +
                    "\"category\" (one of: GENERAL, IT, HR, FACILITIES, PAYROLL, OTHER), " +
                    "\"priority\" (one of: LOW, MEDIUM, HIGH, URGENT), " +
                    "\"explanation\" (short explanation string), " +
                    "\"confidence\" (number between 0.0 and 1.0). " +
                    "Do not include markdown or code fences.";

            String prompt = String.format("Complaint Title: %s\nComplaint Description: %s", title, description);
            String rawJson = callOllamaGenerate(prompt, systemPrompt, true);

            if (rawJson != null) {
                JsonNode root = objectMapper.readTree(rawJson);
                String categoryStr = root.path("category").asText(ComplaintCategory.GENERAL.name()).toUpperCase();
                String priorityStr = root.path("priority").asText(ComplaintPriority.MEDIUM.name()).toUpperCase();
                String explanation = root.path("explanation").asText("Classified using local Ollama model.");
                double confidence = root.path("confidence").asDouble(0.9);

                ComplaintCategory category;
                try {
                    category = ComplaintCategory.valueOf(categoryStr);
                } catch (IllegalArgumentException e) {
                    category = ComplaintCategory.GENERAL;
                }

                ComplaintPriority priority;
                try {
                    priority = ComplaintPriority.valueOf(priorityStr);
                } catch (IllegalArgumentException e) {
                    priority = ComplaintPriority.MEDIUM;
                }

                String suggestedDeptName = null;
                Long suggestedDeptId = null;
                if (departments != null) {
                    final ComplaintCategory finalCategory = category;
                    Department matched = departments.stream()
                            .filter(d -> d.getName().toUpperCase().contains(finalCategory.name()))
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
                        .explanation(explanation)
                        .confidence(confidence)
                        .mode("OLLAMA")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Ollama complaint classification failed, falling back to rule-based: {}", e.getMessage());
        }

        return fallbackProvider.classifyComplaint(title, description, departments);
    }

    @Override
    public TaskSubtaskSuggestionResponse suggestSubtasks(String taskTitle, String taskDescription, TaskPriority priority, LocalDateTime deadline) {
        if (!isAvailable()) {
            return fallbackProvider.suggestSubtasks(taskTitle, taskDescription, priority, deadline);
        }

        try {
            String systemPrompt = "You are a task management AI. Given a task title and description, return a JSON object with: " +
                    "\"suggestedSubtasks\" (JSON array of 3 to 5 clear, concise subtask strings), " +
                    "\"explanation\" (short explanation). Do not use markdown fences.";

            String prompt = String.format("Task Title: %s\nTask Description: %s\nPriority: %s\nDeadline: %s",
                    taskTitle, taskDescription, priority, deadline);

            String rawJson = callOllamaGenerate(prompt, systemPrompt, true);
            if (rawJson != null) {
                JsonNode root = objectMapper.readTree(rawJson);
                List<String> subtasks = new ArrayList<>();
                JsonNode arr = root.path("suggestedSubtasks");
                if (arr.isArray()) {
                    for (JsonNode item : arr) {
                        subtasks.add(item.asText());
                    }
                }
                String explanation = root.path("explanation").asText("Subtasks suggested by Ollama.");

                if (!subtasks.isEmpty()) {
                    return TaskSubtaskSuggestionResponse.builder()
                            .taskTitle(taskTitle)
                            .suggestedSubtasks(subtasks)
                            .explanation(explanation)
                            .mode("OLLAMA")
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("Ollama task subtask suggestion failed, falling back: {}", e.getMessage());
        }

        return fallbackProvider.suggestSubtasks(taskTitle, taskDescription, priority, deadline);
    }

    @Override
    public MeetingNotesSummaryResponse summarizeMeeting(Long meetingId, String notes) {
        if (!isAvailable()) {
            return fallbackProvider.summarizeMeeting(meetingId, notes);
        }

        try {
            String systemPrompt = "You are an executive meeting assistant. Given meeting notes, return a JSON object with: " +
                    "\"summary\" (concise summary paragraph), " +
                    "\"keyPoints\" (JSON array of strings), " +
                    "\"actionItems\" (JSON array of strings). Do not use markdown fences.";

            String rawJson = callOllamaGenerate(notes, systemPrompt, true);
            if (rawJson != null) {
                JsonNode root = objectMapper.readTree(rawJson);
                String summary = root.path("summary").asText();
                List<String> keyPoints = new ArrayList<>();
                JsonNode kpArr = root.path("keyPoints");
                if (kpArr.isArray()) {
                    for (JsonNode item : kpArr) {
                        keyPoints.add(item.asText());
                    }
                }
                List<String> actionItems = new ArrayList<>();
                JsonNode aiArr = root.path("actionItems");
                if (aiArr.isArray()) {
                    for (JsonNode item : aiArr) {
                        actionItems.add(item.asText());
                    }
                }

                if (!summary.isBlank()) {
                    return MeetingNotesSummaryResponse.builder()
                            .meetingId(meetingId)
                            .summary(summary)
                            .keyPoints(keyPoints)
                            .actionItems(actionItems)
                            .mode("OLLAMA")
                            .build();
                }
            }
        } catch (Exception e) {
            log.warn("Ollama meeting summarization failed, falling back: {}", e.getMessage());
        }

        return fallbackProvider.summarizeMeeting(meetingId, notes);
    }

    @Override
    public String generateOfficeAssistantAnswer(String question, String userContext) {
        if (!isAvailable()) {
            return fallbackProvider.generateOfficeAssistantAnswer(question, userContext);
        }

        try {
            String systemPrompt = "You are SmartOffice Assistant. Be polite, concise, and helpful. " +
                    "NEVER reveal passwords, database credentials, security keys, or system secrets. " +
                    "Use the following authorized user context to answer questions:\n" + userContext;

            String response = callOllamaGenerate(question, systemPrompt, false);
            if (response != null && !response.isBlank()) {
                return response.trim();
            }
        } catch (Exception e) {
            log.warn("Ollama assistant generation failed, falling back: {}", e.getMessage());
        }

        return fallbackProvider.generateOfficeAssistantAnswer(question, userContext);
    }

    private String callOllamaGenerate(String prompt, String systemPrompt, boolean jsonFormat) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            if (systemPrompt != null) {
                body.put("system", systemPrompt);
            }
            body.put("stream", false);
            if (jsonFormat) {
                body.put("format", "json");
            }

            String jsonPayload = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofMillis(readTimeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("response").asText(null);
            } else {
                log.debug("Ollama returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.debug("Exception during Ollama generate request: {}", e.getMessage());
        }
        return null;
    }
}

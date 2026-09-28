package com.civicresolve.service.ai;

import com.civicresolve.dto.AiClassificationResult;
import com.civicresolve.entity.ComplaintPriority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component("restAiProvider")
public class RestAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(RestAiProvider.class);

    private final AiProvider fallbackProvider;
    private final ObjectMapper objectMapper;

    @Value("${civicresolve.ai.provider.enabled:false}")
    private boolean enabled;

    @Value("${civicresolve.ai.provider.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${civicresolve.ai.provider.api-key:}")
    private String apiKey;

    @Value("${civicresolve.ai.provider.model:gpt-4o-mini}")
    private String model;

    @Value("${civicresolve.ai.provider.timeout-ms:5000}")
    private int timeoutMs;

    public RestAiProvider(
            @Qualifier("ruleBasedAiProvider") AiProvider fallbackProvider,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.fallbackProvider = fallbackProvider;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public AiClassificationResult classify(String title, String description) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.debug("External REST AI Provider disabled or API key missing. Using fallback provider.");
            return fallbackProvider.classify(title, description);
        }

        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            String systemPrompt = "You are an AI civic grievance classification engine for CivicResolve. " +
                    "Analyze the given complaint title and description and return ONLY a valid JSON object. " +
                    "Do NOT include markdown formatting or backticks. " +
                    "The sector field MUST be EXACTLY one of these 14 canonical sectors: " +
                    String.join(", ", SectorRegistry.CANONICAL_SECTORS) + ". " +
                    "The priority field MUST be one of: LOW, MEDIUM, HIGH, CRITICAL. " +
                    "JSON structure: {\"sector\":\"...\", \"department\":\"...\", \"urgency\":\"...\", \"sentiment\":\"...\", \"priority\":\"...\", \"confidence\":0.85, \"summary\":\"...\"}";

            String userPrompt = "Complaint Title: " + (title != null ? title : "") + "\nDescription: " + (description != null ? description : "");

            Map<String, Object> messageSystem = Map.of("role", "system", "content", systemPrompt);
            Map<String, Object> messageUser = Map.of("role", "user", "content", userPrompt);
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(messageSystem, messageUser),
                    "temperature", 0.1
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody());
            } else {
                log.warn("REST AI API call returned status code {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("External REST AI API call failed: {}. Falling back to RuleBasedAiProvider.", e.getMessage());
        }

        return fallbackProvider.classify(title, description);
    }

    private AiClassificationResult parseResponse(String jsonBody) throws Exception {
        JsonNode root = objectMapper.readTree(jsonBody);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String content = choices.get(0).path("message").path("content").asText();
            if (content != null) {
                String cleanJson = content.trim();
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.substring(7);
                }
                if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.substring(3);
                }
                if (cleanJson.endsWith("```")) {
                    cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                }
                cleanJson = cleanJson.trim();

                JsonNode parsed = objectMapper.readTree(cleanJson);
                String rawSector = parsed.path("sector").asText(null);
                String department = parsed.path("department").asText(null);
                String urgency = parsed.path("urgency").asText("MEDIUM");
                String sentiment = parsed.path("sentiment").asText("NEUTRAL");
                String rawPriority = parsed.path("priority").asText("MEDIUM");
                double confidence = parsed.path("confidence").asDouble(0.85);
                String summary = parsed.path("summary").asText("");

                String canonicalSector = SectorRegistry.normalizeSector(rawSector);

                ComplaintPriority priority;
                try {
                    priority = ComplaintPriority.valueOf(rawPriority.toUpperCase());
                } catch (Exception e) {
                    priority = ComplaintPriority.MEDIUM;
                }

                return AiClassificationResult.builder()
                        .sector(canonicalSector)
                        .department(department)
                        .urgency(urgency)
                        .sentiment(sentiment)
                        .priority(priority)
                        .confidence(confidence)
                        .summary(summary)
                        .build();
            }
        }
        throw new IllegalStateException("Unexpected JSON response structure from REST AI endpoint");
    }
}

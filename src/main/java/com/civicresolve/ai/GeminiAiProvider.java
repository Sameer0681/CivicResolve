package com.civicresolve.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Service
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final boolean enabled;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiAiProvider(
            @Value("${civicresolve.ai.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${civicresolve.ai.gemini.model:${GEMINI_MODEL:gemini-3.6-flash}}") String model,
            @Value("${civicresolve.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${civicresolve.ai.gemini.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${civicresolve.ai.gemini.read-timeout-ms:5000}") int readTimeoutMs,
            @Value("${civicresolve.ai.enabled:true}") boolean enabled) {

        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-3.6-flash";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : "https://generativelanguage.googleapis.com";
        this.enabled = enabled;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 500)));
        factory.setReadTimeout(Duration.ofMillis(Math.max(readTimeoutMs, 500)));

        this.restClient = RestClient.builder()
                .baseUrl(this.baseUrl)
                .requestFactory(factory)
                .build();
    }

    public GeminiAiProvider(String apiKey) {
        this(apiKey, "gemini-3.6-flash", "https://generativelanguage.googleapis.com", 3000, 5000, true);
    }

    public GeminiAiProvider(
            String apiKey,
            String model,
            String baseUrl,
            int connectTimeoutMs,
            int readTimeoutMs,
            boolean enabled,
            RestClient restClient) {

        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "gemini-3.6-flash";
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : "https://generativelanguage.googleapis.com";
        this.enabled = enabled;
        this.objectMapper = new ObjectMapper();
        this.restClient = restClient;
    }

    public boolean isConfigured() {
        return this.enabled && !this.apiKey.isBlank();
    }

    @Override
    public AiClassificationResult classify(String complaintText) {
        if (!this.enabled) {
            log.info("Gemini AI classification is disabled via configuration");
            throw new AiClassificationException("Gemini AI classification is disabled");
        }

        if (this.apiKey.isBlank()) {
            log.info("Gemini AI classification is not configured: API key is missing");
            throw new AiClassificationException("Gemini API key is not configured");
        }

        if (complaintText == null || complaintText.isBlank()) {
            throw new AiClassificationException("Complaint text is empty");
        }

        String prompt = buildPrompt(complaintText);

        GeminiApiRequest apiRequest = new GeminiApiRequest(
                List.of(new GeminiApiRequest.Content(
                        List.of(new GeminiApiRequest.Part(prompt))
                )),
                new GeminiApiRequest.GenerationConfig(0.1, "application/json")
        );

        String requestBodyJson;
        try {
            requestBodyJson = objectMapper.writeValueAsString(apiRequest);
        } catch (Exception e) {
            throw new AiClassificationException("Failed to serialize Gemini request payload", e);
        }

        String endpointUri = "/v1beta/models/" + this.model + ":generateContent";
        String responseJson;

        try {
            responseJson = restClient.post()
                    .uri(endpointUri)
                    .header("x-goog-api-key", this.apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(requestBodyJson)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("Gemini API call failed with HTTP status: {}", e.getStatusCode());
            throw new AiClassificationException("Gemini API HTTP failure: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            log.warn("Gemini API network timeout or connection failure: {}", e.getMessage());
            throw new AiClassificationException("Gemini API network timeout/failure", e);
        } catch (Exception e) {
            log.warn("Gemini API invocation error: {}", e.getClass().getSimpleName());
            throw new AiClassificationException("Gemini API execution error", e);
        }

        if (responseJson == null || responseJson.isBlank()) {
            throw new AiClassificationException("Gemini API returned an empty response body");
        }

        String rawAiText;
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new AiClassificationException("Gemini response contained no candidates");
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new AiClassificationException("Gemini response candidate contained no content parts");
            }
            rawAiText = parts.get(0).path("text").asText();
        } catch (AiClassificationException e) {
            throw e;
        } catch (Exception e) {
            throw new AiClassificationException("Failed to extract content from Gemini response", e);
        }

        if (rawAiText == null || rawAiText.isBlank()) {
            throw new AiClassificationException("Gemini returned empty classification text");
        }

        String cleanJson = stripMarkdownCodeFences(rawAiText);

        AiClassificationResult result;
        try {
            result = objectMapper.readValue(cleanJson, AiClassificationResult.class);
        } catch (Exception e) {
            throw new AiClassificationException("Failed to deserialize Gemini output into AiClassificationResult", e);
        }

        return validateAndSanitizeResult(result);
    }

    private String buildPrompt(String complaintText) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the AI classification engine for CivicResolve citizen grievance management system.\n\n");
        sb.append("Analyze the following citizen complaint and classify it into one of the 14 official civic sectors.\n\n");
        sb.append("Official Sectors and Authoritative Departments:\n");
        CivicSectorRegistry.getSectorToDepartmentMap().forEach((sec, dept) ->
                sb.append("- \"").append(sec).append("\" -> \"").append(dept).append("\"\n")
        );
        sb.append("\nComplaint to analyze:\n");
        sb.append(complaintText).append("\n\n");
        sb.append("Return ONLY valid JSON matching this schema:\n");
        sb.append("{\n");
        sb.append("  \"sector\": \"<Exact official sector from list above>\",\n");
        sb.append("  \"department\": \"<Exact mapped department from list above>\",\n");
        sb.append("  \"urgency\": \"LOW | MEDIUM | HIGH | CRITICAL\",\n");
        sb.append("  \"sentiment\": \"POSITIVE | NEUTRAL | NEGATIVE\",\n");
        sb.append("  \"priority\": \"LOW | MEDIUM | HIGH | CRITICAL\",\n");
        sb.append("  \"confidence\": 0.95,\n");
        sb.append("  \"summary\": \"<Short factual summary under 150 words>\"\n");
        sb.append("}\n\n");
        sb.append("Rules:\n");
        sb.append("- urgency must be LOW, MEDIUM, HIGH, or CRITICAL\n");
        sb.append("- sentiment must be POSITIVE, NEUTRAL, or NEGATIVE\n");
        sb.append("- priority must be LOW, MEDIUM, HIGH, or CRITICAL\n");
        sb.append("- confidence must be a number between 0.0 and 1.0\n");
        sb.append("- Do not use markdown fences. Do not output explanations.\n");
        return sb.toString();
    }

    private String stripMarkdownCodeFences(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    public AiClassificationResult validateAndSanitizeResult(AiClassificationResult result) {
        if (result == null) {
            throw new AiClassificationException("AiClassificationResult is null");
        }

        // 1. Sector validation against official 14 sectors
        String canonicalSector = CivicSectorRegistry.findCanonicalSector(result.getSector());
        if (canonicalSector == null) {
            throw new AiClassificationException("AI returned invalid sector: " + result.getSector());
        }
        result.setSector(canonicalSector);

        // 2. Department validation against mapped department
        String officialDept = CivicSectorRegistry.getDepartmentForSector(canonicalSector);
        if (result.getDepartment() == null || result.getDepartment().isBlank()
                || !CivicSectorRegistry.isDepartmentMatching(canonicalSector, result.getDepartment())) {
            result.setDepartment(officialDept);
        }

        // 3. Urgency validation: LOW, MEDIUM, HIGH, CRITICAL
        result.setUrgency(normalizeUrgencyOrPriority(result.getUrgency()));

        // 4. Priority validation: LOW, MEDIUM, HIGH, CRITICAL
        result.setPriority(normalizeUrgencyOrPriority(result.getPriority()));

        // 5. Sentiment validation: POSITIVE, NEUTRAL, NEGATIVE
        result.setSentiment(normalizeSentiment(result.getSentiment()));

        // 6. Confidence validation: 0.0 to 1.0
        Double conf = result.getConfidence();
        if (conf == null || Double.isNaN(conf) || Double.isInfinite(conf) || conf < 0.0 || conf > 1.0) {
            result.setConfidence(0.0);
        }

        // 7. Summary sanitization and truncation to 1000 characters
        if (result.getSummary() != null) {
            String cleanSummary = result.getSummary().trim();
            cleanSummary = cleanSummary.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
            if (cleanSummary.length() > 1000) {
                cleanSummary = cleanSummary.substring(0, 1000);
            }
            result.setSummary(cleanSummary);
        }

        return result;
    }

    private String normalizeUrgencyOrPriority(String val) {
        if (val == null) {
            return "MEDIUM";
        }
        String upper = val.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> upper;
            default -> "MEDIUM";
        };
    }

    private String normalizeSentiment(String val) {
        if (val == null) {
            return "NEUTRAL";
        }
        String upper = val.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "POSITIVE", "NEUTRAL", "NEGATIVE" -> upper;
            default -> "NEUTRAL";
        };
    }
}

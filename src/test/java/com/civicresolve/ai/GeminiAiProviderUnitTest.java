package com.civicresolve.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiAiProviderUnitTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String MODEL = "gemini-3.6-flash";
    private static final String EXPECTED_URI = BASE_URL + "/v1beta/models/" + MODEL + ":generateContent";

    private MockRestServiceServer mockServer;
    private GeminiAiProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        provider = new GeminiAiProvider(
                "mock-api-key-xyz",
                MODEL,
                BASE_URL,
                2000,
                2000,
                true,
                restClient
        );
    }

    private String buildGeminiResponseBody(String candidateText) {
        String escaped = candidateText.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(escaped);
    }

    @Test
    void testSuccessfulGeminiResponseParsing() {
        String aiJson = """
                {
                  "sector": "Roads & Transport",
                  "department": "Public Works Department (PWD)",
                  "urgency": "HIGH",
                  "sentiment": "NEGATIVE",
                  "priority": "HIGH",
                  "confidence": 0.95,
                  "summary": "Dangerous crater pothole on main road causing hazard"
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "mock-api-key-xyz"))
                .andExpect(header("Content-Type", "application/json"))
                .andRespond(withSuccess(buildGeminiResponseBody(aiJson), MediaType.APPLICATION_JSON));

        AiClassificationResult result = provider.classify("Dangerous potholes on MG road");

        mockServer.verify();
        assertNotNull(result);
        assertEquals("Roads & Transport", result.getSector());
        assertEquals("Public Works Department (PWD)", result.getDepartment());
        assertEquals("HIGH", result.getUrgency());
        assertEquals("NEGATIVE", result.getSentiment());
        assertEquals("HIGH", result.getPriority());
        assertEquals(0.95, result.getConfidence());
        assertTrue(result.getSummary().contains("pothole"));
    }

    @Test
    void testMarkdownFencedJsonResponse() {
        String fencedText = """
                ```json
                {
                  "sector": "Water Supply",
                  "department": "Jal Nigam / Municipal Water Works",
                  "urgency": "CRITICAL",
                  "sentiment": "NEGATIVE",
                  "priority": "CRITICAL",
                  "confidence": 0.88,
                  "summary": "Main pipeline broken and flooding street"
                }
                ```
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(buildGeminiResponseBody(fencedText), MediaType.APPLICATION_JSON));

        AiClassificationResult result = provider.classify("Pipeline burst flooding road");

        mockServer.verify();
        assertNotNull(result);
        assertEquals("Water Supply", result.getSector());
        assertEquals("Jal Nigam / Municipal Water Works", result.getDepartment());
        assertEquals("CRITICAL", result.getUrgency());
        assertEquals("CRITICAL", result.getPriority());
        assertEquals("NEGATIVE", result.getSentiment());
        assertEquals(0.88, result.getConfidence());
    }

    @Test
    void testMissingResponseCandidatesThrowsAiClassificationException() {
        String emptyCandidatesJson = """
                {
                  "candidates": []
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(emptyCandidatesJson, MediaType.APPLICATION_JSON));

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                provider.classify("Valid complaint text"));

        assertTrue(ex.getMessage().contains("no candidates"));
    }

    @Test
    void testMissingTextPartsThrowsAiClassificationException() {
        String emptyPartsJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": []
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(emptyPartsJson, MediaType.APPLICATION_JSON));

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                provider.classify("Valid complaint text"));

        assertTrue(ex.getMessage().contains("no content parts"));
    }

    @Test
    void testMalformedJsonThrowsAiClassificationException() {
        String malformedText = "{ sector: Roads, urgency: }";

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(buildGeminiResponseBody(malformedText), MediaType.APPLICATION_JSON));

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                provider.classify("Valid complaint text"));

        assertTrue(ex.getMessage().contains("deserialize"));
    }

    @Test
    void testHttp400BadRequestThrowsAiClassificationException() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withBadRequest());

        assertThrows(AiClassificationException.class, () ->
                provider.classify("Some complaint"));
        mockServer.verify();
    }

    @Test
    void testHttp401UnauthorizedThrowsAiClassificationException() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withUnauthorizedRequest());

        assertThrows(AiClassificationException.class, () ->
                provider.classify("Some complaint"));
        mockServer.verify();
    }

    @Test
    void testHttp403ForbiddenThrowsAiClassificationExceptionWithoutRetry() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(AiClassificationException.class, () ->
                provider.classify("Some complaint"));
        mockServer.verify();
    }

    @Test
    void testHttp429TooManyRequestsThrowsAiClassificationException() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                provider.classify("Some complaint"));

        assertTrue(ex.getMessage().contains("429"));
        mockServer.verify();
    }

    @Test
    void testHttp500InternalServerErrorThrowsAiClassificationException() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withServerError());

        assertThrows(AiClassificationException.class, () ->
                provider.classify("Some complaint"));
        mockServer.verify();
    }

    @Test
    void testHttp503RetriesAndSucceedsOnNextAttempt() {
        String aiJson = """
                {
                  "sector": "Roads & Transport",
                  "department": "Public Works Department (PWD)",
                  "urgency": "HIGH",
                  "sentiment": "NEGATIVE",
                  "priority": "HIGH",
                  "confidence": 0.95,
                  "summary": "Pothole hazard resolved on retry"
                }
                """;

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer customMock = MockRestServiceServer.bindTo(builder).build();
        GeminiAiProvider fastProvider = new GeminiAiProvider(
                "mock-api-key-xyz",
                MODEL,
                BASE_URL,
                2000,
                2000,
                true,
                2,
                10L,
                builder.build()
        );

        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(buildGeminiResponseBody(aiJson), MediaType.APPLICATION_JSON));

        AiClassificationResult result = fastProvider.classify("Pothole on main road");

        customMock.verify();
        assertNotNull(result);
        assertEquals("Roads & Transport", result.getSector());
        assertEquals(0.95, result.getConfidence());
        assertTrue(result.getSummary().contains("Pothole"));
    }

    @Test
    void testHttp503SucceedsOnSecondRetry() {
        String aiJson = """
                {
                  "sector": "Water Supply",
                  "department": "Jal Nigam / Municipal Water Works",
                  "urgency": "HIGH",
                  "sentiment": "NEGATIVE",
                  "priority": "HIGH",
                  "confidence": 0.98,
                  "summary": "Water supply outage resolved on second retry"
                }
                """;

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer customMock = MockRestServiceServer.bindTo(builder).build();
        GeminiAiProvider fastProvider = new GeminiAiProvider(
                "mock-api-key-xyz",
                MODEL,
                BASE_URL,
                2000,
                2000,
                true,
                2,
                10L,
                builder.build()
        );

        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(buildGeminiResponseBody(aiJson), MediaType.APPLICATION_JSON));

        AiClassificationResult result = fastProvider.classify("Water supply outage for three days");

        customMock.verify();
        assertNotNull(result);
        assertEquals("Water Supply", result.getSector());
        assertEquals("Jal Nigam / Municipal Water Works", result.getDepartment());
        assertEquals(0.98, result.getConfidence());
    }

    @Test
    void testHttp503RepeatedFailuresExhaustsRetries() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer customMock = MockRestServiceServer.bindTo(builder).build();
        GeminiAiProvider fastProvider = new GeminiAiProvider(
                "mock-api-key-xyz",
                MODEL,
                BASE_URL,
                2000,
                2000,
                true,
                2,
                10L,
                builder.build()
        );

        // Initial attempt + 2 retries = 3 total attempts
        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        customMock.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                fastProvider.classify("Water supply interrupted for three days"));

        customMock.verify();
        assertTrue(ex.getMessage().contains("503") || ex.getMessage().contains("HTTP failure"));
    }

    @Test
    void testNetworkFailureThrowsAiClassificationException() {
        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(request -> {
                    throw new org.springframework.web.client.ResourceAccessException("Read timed out", new SocketTimeoutException("timeout"));
                });

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                provider.classify("Some complaint"));

        assertTrue(ex.getMessage().contains("timeout") || ex.getMessage().contains("failure"));
    }

    @Test
    void testMissingApiKeyThrowsAiClassificationExceptionWithoutNetworkCall() {
        GeminiAiProvider unconfigured = new GeminiAiProvider(
                "",
                MODEL,
                BASE_URL,
                2000,
                2000,
                true,
                RestClient.builder().baseUrl(BASE_URL).build()
        );

        assertFalse(unconfigured.isConfigured());

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                unconfigured.classify("Valid complaint text"));

        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    void testDisabledAiThrowsAiClassificationExceptionWithoutNetworkCall() {
        GeminiAiProvider disabledProvider = new GeminiAiProvider(
                "some-key",
                MODEL,
                BASE_URL,
                2000,
                2000,
                false,
                RestClient.builder().baseUrl(BASE_URL).build()
        );

        assertFalse(disabledProvider.isConfigured());

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                disabledProvider.classify("Valid complaint text"));

        assertTrue(ex.getMessage().contains("disabled"));
    }

    @Test
    void testInvalidSectorThrowsAiClassificationException() {
        String invalidSectorJson = """
                {
                  "sector": "Alien Encounters Department",
                  "department": "Extraterrestrial Affairs",
                  "urgency": "HIGH",
                  "priority": "HIGH"
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(buildGeminiResponseBody(invalidSectorJson), MediaType.APPLICATION_JSON));

        AiClassificationException ex = assertThrows(AiClassificationException.class, () ->
                provider.classify("UFO landed"));

        assertTrue(ex.getMessage().contains("invalid sector"));
    }

    @Test
    void testUnmatchedDepartmentIsNormalizedToOfficialSectorDepartment() {
        String mismatchedDeptJson = """
                {
                  "sector": "Roads & Transport",
                  "department": "Random Ministry of Space",
                  "urgency": "MEDIUM",
                  "priority": "MEDIUM",
                  "confidence": 0.8
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(buildGeminiResponseBody(mismatchedDeptJson), MediaType.APPLICATION_JSON));

        AiClassificationResult result = provider.classify("Pothole problem");

        assertEquals("Roads & Transport", result.getSector());
        assertEquals("Public Works Department (PWD)", result.getDepartment());
    }

    @Test
    void testNormalizesInvalidUrgencyPrioritySentimentAndConfidence() {
        String weirdEnumsJson = """
                {
                  "sector": "Street Lighting",
                  "department": "Municipal Electrical & Lighting Cell",
                  "urgency": "SUPER_URGENT",
                  "sentiment": "FURIOUS",
                  "priority": "MAXIMUM",
                  "confidence": 2.5,
                  "summary": "Dark pole"
                }
                """;

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(buildGeminiResponseBody(weirdEnumsJson), MediaType.APPLICATION_JSON));

        AiClassificationResult result = provider.classify("Street light is dark");

        assertEquals("Street Lighting", result.getSector());
        assertEquals("MEDIUM", result.getUrgency());
        assertEquals("MEDIUM", result.getPriority());
        assertEquals("NEUTRAL", result.getSentiment());
        assertEquals(0.0, result.getConfidence());
    }

    @Test
    void testOversizedSummaryIsTruncatedTo1000Chars() {
        String hugeSummary = "A".repeat(1500);
        String hugeSummaryJson = """
                {
                  "sector": "Sanitation & Waste",
                  "department": "Municipal Solid Waste Management Division",
                  "urgency": "LOW",
                  "priority": "LOW",
                  "confidence": 0.9,
                  "summary": "%s"
                }
                """.formatted(hugeSummary);

        mockServer.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(buildGeminiResponseBody(hugeSummaryJson), MediaType.APPLICATION_JSON));

        AiClassificationResult result = provider.classify("Garbage pile");

        assertNotNull(result.getSummary());
        assertEquals(1000, result.getSummary().length());
    }
}

package com.civicresolve.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GeminiAiProvider implements AiProvider {

  private final RestClient restClient;
  private final String apiKey;
  private final ObjectMapper objectMapper;

  public GeminiAiProvider(
      @Value("${GEMINI_API_KEY}") String apiKey) {

    this.restClient = RestClient.builder()
        .baseUrl("https://generativelanguage.googleapis.com")
        .build();

    this.apiKey = apiKey;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public AiClassificationResult classify(String complaintText) {
    String prompt = """
        You are the AI classification engine for CivicResolve.

        Analyze this citizen complaint:

        %s

        Return ONLY valid JSON.
        Do not use markdown.
        Do not add explanations.

        Use exactly these fields:

        {
          "sector": "Roads & Transport",
          "department": "Public Works Department",
          "urgency": "HIGH",
          "sentiment": "NEGATIVE",
          "priority": "HIGH",
          "confidence": 0.95,
          "summary": "Citizen reports dangerous potholes on the street."
        }

        Rules:
        - urgency must be LOW, MEDIUM, HIGH, or CRITICAL
        - sentiment must be POSITIVE, NEUTRAL, or NEGATIVE
        - priority must be LOW, MEDIUM, HIGH, or CRITICAL
        - confidence must be a number between 0 and 1
        - summary must be a short summary of the complaint
        """.formatted(complaintText);

    String body = """
        {
          "contents": [
            {
              "parts": [
                {
                  "text": %s
                }
              ]
            }
          ]
        }
        """.formatted(
        "\"" + prompt.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n") + "\"");

    String response = restClient.post()
        .uri("/v1beta/models/gemini-3.6-flash:generateContent")
        .header("x-goog-api-key", apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .body(body)
        .retrieve()
        .body(String.class);

    System.out.println("Gemini Response:");
    System.out.println(response);

    System.out.println("Gemini Response:");
    System.out.println(response);

    try {
      JsonNode root = objectMapper.readTree(response);

      String aiText = root
          .path("candidates")
          .get(0)
          .path("content")
          .path("parts")
          .get(0)
          .path("text")
          .asText();

      AiClassificationResult result = objectMapper.readValue(aiText, AiClassificationResult.class);

      System.out.println("Parsed AI Result:");
      System.out.println(result);

      return result;

    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Gemini response", e);
    }
  }

}

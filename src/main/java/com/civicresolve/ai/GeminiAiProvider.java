package com.civicresolve.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service 
public class GeminiAiProvider implements AiProvider {

    private final RestClient restClient;
    private final String apiKey;

    public GeminiAiProvider(
            @Value("${GEMINI_API_KEY}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        this.apiKey = apiKey;
    }
    
    @Override
    public AiClassificationResult classify(String complaintText) {
         String body = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "Say hello in one sentence."
                    }
                  ]
                }
              ]
            }
            """;

    String response = restClient.post()
        .uri("/v1beta/models/gemini-3.8-flash:generateContent")
        .header("x-goog-api-key", apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .body(body)
        .retrieve()
        .body(String.class);

    System.out.println("Gemini Response:");
    System.out.println(response);

    return null;
    }
    
}

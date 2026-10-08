package com.civicresolve.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.civicresolve.ai.AiClassificationResult;
import com.civicresolve.ai.GeminiAiProvider;

@SpringBootTest
class GeminiAiProviderTest {

    @Autowired
    private GeminiAiProvider geminiAiProvider;

    @Test
    void testGeminiConnection() {

        String complaint = "There are dangerous potholes on my street.";

        AiClassificationResult result =
                geminiAiProvider.classify(complaint);

        System.out.println("Test completed.");
    }
}
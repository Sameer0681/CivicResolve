package com.civicresolve.ai;

/**
 * Controlled exception representing expected Gemini or AI classification failures
 * (e.g., missing API key, network timeout, rate limit, quota exhaustion, malformed model response).
 */
public class AiClassificationException extends RuntimeException {

    public AiClassificationException(String message) {
        super(message);
    }

    public AiClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

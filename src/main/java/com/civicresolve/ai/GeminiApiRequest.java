package com.civicresolve.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Structured request payload for Google Gemini generateContent REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiApiRequest {

    private List<Content> contents;
    private GenerationConfig generationConfig;

    public GeminiApiRequest() {
    }

    public GeminiApiRequest(List<Content> contents, GenerationConfig generationConfig) {
        this.contents = contents;
        this.generationConfig = generationConfig;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    public static class Content {
        private List<Part> parts;

        public Content() {
        }

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class GenerationConfig {
        private Double temperature;
        private String responseMimeType;

        public GenerationConfig() {
        }

        public GenerationConfig(Double temperature, String responseMimeType) {
            this.temperature = temperature;
            this.responseMimeType = responseMimeType;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public String getResponseMimeType() {
            return responseMimeType;
        }

        public void setResponseMimeType(String responseMimeType) {
            this.responseMimeType = responseMimeType;
        }
    }
}

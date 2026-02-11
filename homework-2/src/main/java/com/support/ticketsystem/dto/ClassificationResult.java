package com.support.ticketsystem.dto;

import java.util.List;

public class ClassificationResult {
    private String category;
    private String priority;
    private double confidence;
    private String reasoning;
    private List<String> keywordsFound;

    public ClassificationResult() {}

    public ClassificationResult(String category, String priority, double confidence, String reasoning, List<String> keywordsFound) {
        this.category = category;
        this.priority = priority;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.keywordsFound = keywordsFound;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public List<String> getKeywordsFound() {
        return keywordsFound;
    }

    public void setKeywordsFound(List<String> keywordsFound) {
        this.keywordsFound = keywordsFound;
    }
}

package com.support.ticketsystem.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClassificationData {
    private String category;
    private String priority;
    private Double confidence;
    private String reasoning;
    
    @JsonProperty("auto_classified")
    private Boolean autoClassified;

    public ClassificationData() {}

    public ClassificationData(String category, String priority, Double confidence, String reasoning, Boolean autoClassified) {
        this.category = category;
        this.priority = priority;
        this.confidence = confidence;
        this.reasoning = reasoning;
        this.autoClassified = autoClassified;
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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Boolean getAutoClassified() {
        return autoClassified;
    }

    public void setAutoClassified(Boolean autoClassified) {
        this.autoClassified = autoClassified;
    }
}

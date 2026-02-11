package com.support.ticketsystem.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.support.ticketsystem.dto.ClassificationResult;
import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Ticket;

@Service
public class AutoClassificationService {
    private static final Logger logger = LoggerFactory.getLogger(AutoClassificationService.class);

    private static final Map<Category, List<String>> CATEGORY_KEYWORDS = Map.of(
            Category.ACCOUNT_ACCESS, List.of("login", "password", "2fa", "two-factor", "authentication", 
                    "sign in", "access", "locked", "unlock", "credentials", "verify", "verification"),
            Category.TECHNICAL_ISSUE, List.of("bug", "error", "crash", "not working", "broken", 
                    "issue", "problem", "fail", "timeout", "slow", "performance", "loading"),
            Category.BILLING_QUESTION, List.of("billing", "payment", "invoice", "charge", "refund", 
                    "subscription", "price", "cost", "pay", "credit card", "overcharge"),
            Category.FEATURE_REQUEST, List.of("feature", "enhancement", "suggest", "request", "add", 
                    "improve", "would like", "need", "wish", "could you"),
            Category.BUG_REPORT, List.of("bug", "defect", "reproduce", "steps to reproduce", "expected", 
                    "actual", "incorrect", "wrong", "consistently")
    );

    private static final Map<Priority, List<String>> PRIORITY_KEYWORDS = Map.of(
            Priority.URGENT, List.of("can't access", "critical", "production down", "security", 
                    "urgent", "immediately", "emergency", "asap", "right now"),
            Priority.HIGH, List.of("important", "blocking", "asap", "soon", "priority", "need", 
                    "must", "cannot", "stuck"),
            Priority.LOW, List.of("minor", "cosmetic", "suggestion", "nice to have", "eventually", 
                    "when possible", "low priority")
    );

    public ClassificationResult classifyTicket(Ticket ticket) {
        String text = (ticket.getSubject() + " " + ticket.getDescription()).toLowerCase();
        
        // Classify category
        Map<Category, Integer> categoryScores = new HashMap<>();
        List<String> foundKeywords = new ArrayList<>();
        
        for (Map.Entry<Category, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase())) {
                    score++;
                    foundKeywords.add(keyword);
                }
            }
            if (score > 0) {
                categoryScores.put(entry.getKey(), score);
            }
        }
        
        Category bestCategory = categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Category.OTHER);
        
        // Classify priority
        Priority detectedPriority = Priority.MEDIUM; // default
        List<String> priorityKeywords = new ArrayList<>();
        
        for (Map.Entry<Priority, List<String>> entry : PRIORITY_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword.toLowerCase())) {
                    detectedPriority = entry.getKey();
                    priorityKeywords.add(keyword);
                    break;
                }
            }
            if (!priorityKeywords.isEmpty()) {
                break;
            }
        }
        
        // Calculate confidence score
        int totalKeywords = CATEGORY_KEYWORDS.getOrDefault(bestCategory, List.of()).size();
        int matchedKeywords = categoryScores.getOrDefault(bestCategory, 0);
        double confidence = totalKeywords > 0 
            ? Math.min(1.0, (double) matchedKeywords / Math.max(1, totalKeywords / 2))
            : 0.2;  // Default confidence for OTHER category
        
        // Build reasoning
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Category '").append(bestCategory).append("' detected based on keywords: ");
        reasoning.append(String.join(", ", foundKeywords.stream().distinct().limit(5).collect(Collectors.toList())));
        reasoning.append(". Priority '").append(detectedPriority).append("'");
        if (!priorityKeywords.isEmpty()) {
            reasoning.append(" based on keywords: ").append(String.join(", ", priorityKeywords));
        } else {
            reasoning.append(" (default)");
        }
        
        ClassificationResult result = new ClassificationResult(
                bestCategory.name(),
                detectedPriority.name(),
                confidence,
                reasoning.toString(),
                foundKeywords.stream().distinct().collect(Collectors.toList())
        );
        
        // Log the decision
        logger.info("Auto-classified ticket {}: category={}, priority={}, confidence={}", 
                ticket.getId(), bestCategory, detectedPriority, confidence);
        
        return result;
    }

    public void applyClassification(Ticket ticket, ClassificationResult result) {
        ticket.setCategory(Category.valueOf(result.getCategory()));
        ticket.setPriority(Priority.valueOf(result.getPriority()));
        
        // Store classification data if field exists in ticket model
        if (ticket.getClassificationData() == null) {
            ticket.setClassificationData(new com.support.ticketsystem.model.ClassificationData());
        }
        ticket.getClassificationData().setCategory(result.getCategory());
        ticket.getClassificationData().setPriority(result.getPriority());
        ticket.getClassificationData().setConfidence(result.getConfidence());
        ticket.getClassificationData().setReasoning(result.getReasoning());
        ticket.getClassificationData().setAutoClassified(true);
        
        logger.info("Applied classification to ticket {}", ticket.getId());
    }
}

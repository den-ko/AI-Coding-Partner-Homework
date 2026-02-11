package com.support.ticketsystem.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.support.ticketsystem.dto.ClassificationResult;
import com.support.ticketsystem.model.Ticket;
import com.support.ticketsystem.service.AutoClassificationService;
import com.support.ticketsystem.service.TicketService;

@RestController
@RequestMapping("/tickets")
public class ClassificationController {
    private final TicketService ticketService;
    private final AutoClassificationService autoClassificationService;

    public ClassificationController(TicketService ticketService, AutoClassificationService autoClassificationService) {
        this.ticketService = ticketService;
        this.autoClassificationService = autoClassificationService;
    }

    @PostMapping("/{id}/auto-classify")
    public ResponseEntity<ClassificationResult> autoClassifyTicket(@PathVariable UUID id) {
        Ticket ticket = ticketService.getTicketById(id);
        
        ClassificationResult result = autoClassificationService.classifyTicket(ticket);
        
        // Apply classification to the ticket
        autoClassificationService.applyClassification(ticket, result);
        ticketService.updateTicket(id, ticket);
        
        return ResponseEntity.ok(result);
    }
}

package com.support.ticketsystem.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Status;
import com.support.ticketsystem.model.Ticket;
import com.support.ticketsystem.service.AutoClassificationService;
import com.support.ticketsystem.service.TicketService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/tickets")
public class TicketController {
    private final TicketService ticketService;
    private final AutoClassificationService autoClassificationService;

    public TicketController(TicketService ticketService, AutoClassificationService autoClassificationService) {
        this.ticketService = ticketService;
        this.autoClassificationService = autoClassificationService;
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @Valid @RequestBody Ticket ticket,
            @RequestParam(required = false, defaultValue = "false") boolean autoClassify) {
        
        if (autoClassify) {
            var result = autoClassificationService.classifyTicket(ticket);
            autoClassificationService.applyClassification(ticket, result);
        }
        
        Ticket createdTicket = ticketService.createTicket(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Status status) {
        List<Ticket> tickets = ticketService.getAllTickets(category, priority, status);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable UUID id) {
        Ticket ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody Ticket ticket) {
        Ticket updatedTicket = ticketService.updateTicket(id, ticket);
        return ResponseEntity.ok(updatedTicket);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}

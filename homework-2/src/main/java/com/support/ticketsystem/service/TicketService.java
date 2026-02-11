package com.support.ticketsystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.support.ticketsystem.exception.TicketNotFoundException;
import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Status;
import com.support.ticketsystem.model.Ticket;
import com.support.ticketsystem.repository.TicketRepository;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket createTicket(Ticket ticket) {
        if (ticket.getId() == null) {
            ticket.setId(UUID.randomUUID());
        }
        if (ticket.getCreatedAt() == null) {
            ticket.setCreatedAt(LocalDateTime.now());
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        if (ticket.getStatus() == null) {
            ticket.setStatus(Status.NEW);
        }
        return ticketRepository.save(ticket);
    }

    public Ticket getTicketById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
    }

    public List<Ticket> getAllTickets(Category category, Priority priority, Status status) {
        if (category == null && priority == null && status == null) {
            return ticketRepository.findAll();
        }
        return ticketRepository.findByFilters(category, priority, status);
    }

    public Ticket updateTicket(UUID id, Ticket updatedTicket) {
        Ticket existingTicket = getTicketById(id);
        
        existingTicket.setCustomerId(updatedTicket.getCustomerId());
        existingTicket.setCustomerEmail(updatedTicket.getCustomerEmail());
        existingTicket.setCustomerName(updatedTicket.getCustomerName());
        existingTicket.setSubject(updatedTicket.getSubject());
        existingTicket.setDescription(updatedTicket.getDescription());
        existingTicket.setCategory(updatedTicket.getCategory());
        existingTicket.setPriority(updatedTicket.getPriority());
        existingTicket.setStatus(updatedTicket.getStatus());
        existingTicket.setAssignedTo(updatedTicket.getAssignedTo());
        existingTicket.setTags(updatedTicket.getTags());
        existingTicket.setMetadata(updatedTicket.getMetadata());
        existingTicket.setUpdatedAt(LocalDateTime.now());
        
        if (updatedTicket.getStatus() == Status.RESOLVED && existingTicket.getResolvedAt() == null) {
            existingTicket.setResolvedAt(LocalDateTime.now());
        }
        
        return ticketRepository.save(existingTicket);
    }

    public void deleteTicket(UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException("Ticket not found with id: " + id);
        }
        ticketRepository.deleteById(id);
    }
}

package com.support.ticketsystem.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Status;
import com.support.ticketsystem.model.Ticket;

@Repository
public class TicketRepository {
    private final Map<UUID, Ticket> tickets = new ConcurrentHashMap<>();

    public Ticket save(Ticket ticket) {
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    public Optional<Ticket> findById(UUID id) {
        return Optional.ofNullable(tickets.get(id));
    }

    public List<Ticket> findAll() {
        return new ArrayList<>(tickets.values());
    }

    public List<Ticket> findByFilters(Category category, Priority priority, Status status) {
        return tickets.values().stream()
                .filter(ticket -> category == null || ticket.getCategory() == category)
                .filter(ticket -> priority == null || ticket.getPriority() == priority)
                .filter(ticket -> status == null || ticket.getStatus() == status)
                .collect(Collectors.toList());
    }

    public void deleteById(UUID id) {
        tickets.remove(id);
    }

    public boolean existsById(UUID id) {
        return tickets.containsKey(id);
    }

    public void deleteAll() {
        tickets.clear();
    }

    public long count() {
        return tickets.size();
    }
}

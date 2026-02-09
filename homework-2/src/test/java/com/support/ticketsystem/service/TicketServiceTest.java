package com.support.ticketsystem.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.support.ticketsystem.exception.TicketNotFoundException;
import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.DeviceType;
import com.support.ticketsystem.model.Metadata;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Source;
import com.support.ticketsystem.model.Status;
import com.support.ticketsystem.model.Ticket;
import com.support.ticketsystem.repository.TicketRepository;

class TicketServiceTest {

    private TicketRepository ticketRepository;
    private TicketService ticketService;
    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        ticketService = new TicketService(ticketRepository);
        sampleTicket = createSampleTicket();
    }

    @Test
    void createTicket_withNullId_generatesUUID() {
        sampleTicket.setId(null);

        Ticket created = ticketService.createTicket(sampleTicket);

        assertThat(created.getId()).isNotNull();
    }

    @Test
    void createTicket_withExistingId_preservesId() {
        UUID existingId = UUID.randomUUID();
        sampleTicket.setId(existingId);

        Ticket created = ticketService.createTicket(sampleTicket);

        assertThat(created.getId()).isEqualTo(existingId);
    }

    @Test
    void createTicket_withNullCreatedAt_setsTimestamp() {
        sampleTicket.setCreatedAt(null);
        sampleTicket.setUpdatedAt(null);

        Ticket created = ticketService.createTicket(sampleTicket);

        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void createTicket_withNullStatus_defaultsToNew() {
        sampleTicket.setStatus(null);

        Ticket created = ticketService.createTicket(sampleTicket);

        assertThat(created.getStatus()).isEqualTo(Status.NEW);
    }

    @Test
    void getTicketById_existingTicket_returnsTicket() {
        UUID ticketId = UUID.randomUUID();
        sampleTicket.setId(ticketId);
        ticketRepository.save(sampleTicket);

        Ticket found = ticketService.getTicketById(ticketId);

        assertThat(found).isEqualTo(sampleTicket);
    }

    @Test
    void getTicketById_nonExistent_throwsException() {
        UUID ticketId = UUID.randomUUID();

        assertThatThrownBy(() -> ticketService.getTicketById(ticketId))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining(ticketId.toString());
    }

    @Test
    void getAllTickets_withNoFilters_returnsAllTickets() {
        Ticket ticket1 = createSampleTicket();
        Ticket ticket2 = createSampleTicket();
        ticketRepository.save(ticket1);
        ticketRepository.save(ticket2);

        List<Ticket> found = ticketService.getAllTickets(null, null, null);

        assertThat(found).hasSize(2);
    }

    @Test
    void getAllTickets_withFilters_callsFilteredFind() {
        Ticket bugTicket = createSampleTicket();
        bugTicket.setCategory(Category.BUG_REPORT);
        bugTicket.setPriority(Priority.HIGH);
        bugTicket.setStatus(Status.NEW);
        ticketRepository.save(bugTicket);
        
        Ticket featureTicket = createSampleTicket();
        featureTicket.setCategory(Category.FEATURE_REQUEST);
        ticketRepository.save(featureTicket);

        List<Ticket> found = ticketService.getAllTickets(Category.BUG_REPORT, Priority.HIGH, Status.NEW);

        assertThat(found).hasSize(1);
    }

    @Test
    void updateTicket_existingTicket_updatesAllFields() {
        UUID ticketId = UUID.randomUUID();
        sampleTicket.setId(ticketId);
        ticketRepository.save(sampleTicket);
        
        Ticket updatedTicket = createSampleTicket();
        updatedTicket.setSubject("Updated Subject");
        updatedTicket.setStatus(Status.IN_PROGRESS);

        Ticket result = ticketService.updateTicket(ticketId, updatedTicket);

        assertThat(result.getSubject()).isEqualTo("Updated Subject");
    }

    @Test
    void updateTicket_statusChangedToResolved_setsResolvedAt() {
        UUID ticketId = UUID.randomUUID();
        sampleTicket.setId(ticketId);
        sampleTicket.setStatus(Status.IN_PROGRESS);
        sampleTicket.setResolvedAt(null);
        ticketRepository.save(sampleTicket);
        
        Ticket updatedTicket = createSampleTicket();
        updatedTicket.setStatus(Status.RESOLVED);

        Ticket result = ticketService.updateTicket(ticketId, updatedTicket);

        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void updateTicket_nonExistent_throwsException() {
        UUID ticketId = UUID.randomUUID();

        assertThatThrownBy(() -> ticketService.updateTicket(ticketId, sampleTicket))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void deleteTicket_existingTicket_deletesSuccessfully() {
        UUID ticketId = UUID.randomUUID();
        sampleTicket.setId(ticketId);
        ticketRepository.save(sampleTicket);

        ticketService.deleteTicket(ticketId);

        assertThat(ticketRepository.findById(ticketId)).isEmpty();
    }

    @Test
    void deleteTicket_nonExistent_throwsException() {
        UUID ticketId = UUID.randomUUID();

        assertThatThrownBy(() -> ticketService.deleteTicket(ticketId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    // Helper method
    private Ticket createSampleTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setCustomerId("CUST001");
        ticket.setCustomerEmail("test@example.com");
        ticket.setCustomerName("Test User");
        ticket.setSubject("Test Subject");
        ticket.setDescription("This is a test description with enough characters");
        ticket.setCategory(Category.TECHNICAL_ISSUE);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setStatus(Status.NEW);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setTags(Arrays.asList("test"));
        
        Metadata metadata = new Metadata();
        metadata.setSource(Source.WEB_FORM);
        metadata.setBrowser("Chrome");
        metadata.setDeviceType(DeviceType.DESKTOP);
        ticket.setMetadata(metadata);
        
        return ticket;
    }
}

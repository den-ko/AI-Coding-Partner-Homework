package com.support.ticketsystem.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.DeviceType;
import com.support.ticketsystem.model.Metadata;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Source;
import com.support.ticketsystem.model.Status;
import com.support.ticketsystem.model.Ticket;

class TicketRepositoryTest {

    private TicketRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TicketRepository();
        repository.deleteAll(); // Clean slate for each test
    }

    @Test
    void save_newTicket_storesSuccessfully() {
        Ticket ticket = createSampleTicket();

        Ticket saved = repository.save(ticket);

        assertThat(saved).isEqualTo(ticket);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void save_existingTicket_overwritesData() {
        Ticket ticket = createSampleTicket();
        UUID ticketId = ticket.getId();
        repository.save(ticket);

        ticket.setSubject("Updated Subject");
        repository.save(ticket);

        Optional<Ticket> found = repository.findById(ticketId);
        assertThat(found).isPresent();
        assertThat(found.get().getSubject()).isEqualTo("Updated Subject");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void findById_existingTicket_returnsTicket() {
        Ticket ticket = createSampleTicket();
        UUID ticketId = ticket.getId();
        repository.save(ticket);

        Optional<Ticket> found = repository.findById(ticketId);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(ticketId);
    }

    @Test
    void findById_nonExistent_returnsEmpty() {
        UUID randomId = UUID.randomUUID();

        Optional<Ticket> found = repository.findById(randomId);

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_returnsAllTickets() {
        Ticket ticket1 = createSampleTicket();
        Ticket ticket2 = createSampleTicket();
        repository.save(ticket1);
        repository.save(ticket2);

        List<Ticket> all = repository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).contains(ticket1, ticket2);
    }

    @Test
    void findByFilters_categoryFilter_returnsMatchingTickets() {
        Ticket bugTicket = createSampleTicket();
        bugTicket.setCategory(Category.BUG_REPORT);
        
        Ticket featureTicket = createSampleTicket();
        featureTicket.setCategory(Category.FEATURE_REQUEST);
        
        repository.save(bugTicket);
        repository.save(featureTicket);

        List<Ticket> filtered = repository.findByFilters(Category.BUG_REPORT, null, null);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getCategory()).isEqualTo(Category.BUG_REPORT);
    }

    @Test
    void findByFilters_priorityFilter_returnsMatchingTickets() {
        Ticket urgentTicket = createSampleTicket();
        urgentTicket.setPriority(Priority.URGENT);
        
        Ticket lowTicket = createSampleTicket();
        lowTicket.setPriority(Priority.LOW);
        
        repository.save(urgentTicket);
        repository.save(lowTicket);

        List<Ticket> filtered = repository.findByFilters(null, Priority.URGENT, null);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getPriority()).isEqualTo(Priority.URGENT);
    }

    @Test
    void findByFilters_multipleFilters_returnsMatchingTickets() {
        Ticket match = createSampleTicket();
        match.setCategory(Category.BUG_REPORT);
        match.setPriority(Priority.HIGH);
        match.setStatus(Status.NEW);
        
        Ticket noMatch1 = createSampleTicket();
        noMatch1.setCategory(Category.BUG_REPORT);
        noMatch1.setPriority(Priority.LOW);
        noMatch1.setStatus(Status.NEW);
        
        Ticket noMatch2 = createSampleTicket();
        noMatch2.setCategory(Category.FEATURE_REQUEST);
        noMatch2.setPriority(Priority.HIGH);
        noMatch2.setStatus(Status.NEW);
        
        repository.save(match);
        repository.save(noMatch1);
        repository.save(noMatch2);

        List<Ticket> filtered = repository.findByFilters(Category.BUG_REPORT, Priority.HIGH, Status.NEW);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0)).isEqualTo(match);
    }

    @Test
    void deleteById_existingTicket_removesSuccessfully() {
        Ticket ticket = createSampleTicket();
        UUID ticketId = ticket.getId();
        repository.save(ticket);

        repository.deleteById(ticketId);

        assertThat(repository.findById(ticketId)).isEmpty();
        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    void existsById_existingTicket_returnsTrue() {
        Ticket ticket = createSampleTicket();
        UUID ticketId = ticket.getId();
        repository.save(ticket);

        boolean exists = repository.existsById(ticketId);

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_nonExistent_returnsFalse() {
        UUID randomId = UUID.randomUUID();

        boolean exists = repository.existsById(randomId);

        assertThat(exists).isFalse();
    }

    @Test
    void deleteAll_clearsRepository() {
        repository.save(createSampleTicket());
        repository.save(createSampleTicket());
        repository.save(createSampleTicket());

        repository.deleteAll();

        assertThat(repository.count()).isEqualTo(0);
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void count_returnsCorrectSize() {
        assertThat(repository.count()).isEqualTo(0);

        repository.save(createSampleTicket());
        assertThat(repository.count()).isEqualTo(1);

        repository.save(createSampleTicket());
        assertThat(repository.count()).isEqualTo(2);
    }

    // Helper method
    private Ticket createSampleTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setCustomerId("CUST" + System.nanoTime());
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

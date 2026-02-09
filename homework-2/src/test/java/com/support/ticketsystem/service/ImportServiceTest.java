package com.support.ticketsystem.service;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.support.ticketsystem.dto.ImportResult;
import com.support.ticketsystem.exception.InvalidFileFormatException;
import com.support.ticketsystem.model.Ticket;
import com.support.ticketsystem.parser.TicketParser;
import com.support.ticketsystem.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    private TicketRepository ticketRepository;

    @Mock
    private TicketParser csvParser;

    @Mock
    private TicketParser jsonParser;

    @Mock
    private MultipartFile file;

    private ImportService importService;

    @BeforeEach
    void setUp() {
        ticketRepository = new TicketRepository();
        importService = new ImportService(Arrays.asList(csvParser, jsonParser), ticketRepository);
    }

    @Test
    void importTickets_emptyFile_throwsException() {
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> importService.importTickets(file, "csv"))
                .isInstanceOf(InvalidFileFormatException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void importTickets_unsupportedFormat_throwsException() {
        when(file.isEmpty()).thenReturn(false);
        when(csvParser.supports("txt")).thenReturn(false);
        when(jsonParser.supports("txt")).thenReturn(false);

        assertThatThrownBy(() -> importService.importTickets(file, "txt"))
                .isInstanceOf(InvalidFileFormatException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void importTickets_csvFormat_callsCsvParser() {
        when(file.isEmpty()).thenReturn(false);
        when(csvParser.supports("csv")).thenReturn(true);
        
        ImportResult mockResult = new ImportResult();
        mockResult.setTotal(2);
        mockResult.setSuccessful(2);
        
        List<Ticket> tickets = Arrays.asList(new Ticket(), new Ticket());
        when(csvParser.parse(eq(file), any(ImportResult.class))).thenReturn(tickets);

        ImportResult result = importService.importTickets(file, "csv");

        assertThat(result).isNotNull();
        verify(csvParser).supports("csv");
        verify(csvParser).parse(eq(file), any(ImportResult.class));
        assertThat(ticketRepository.count()).isEqualTo(2);
    }

    @Test
    void importTickets_jsonFormat_callsJsonParser() {
        when(file.isEmpty()).thenReturn(false);
        when(csvParser.supports("json")).thenReturn(false);
        when(jsonParser.supports("json")).thenReturn(true);
        
        ImportResult mockResult = new ImportResult();
        mockResult.setTotal(1);
        mockResult.setSuccessful(1);
        
        List<Ticket> tickets = Arrays.asList(new Ticket());
        when(jsonParser.parse(eq(file), any(ImportResult.class))).thenReturn(tickets);

        ImportResult result = importService.importTickets(file, "json");

        assertThat(result).isNotNull();
        verify(jsonParser).supports("json");
        verify(jsonParser).parse(eq(file), any(ImportResult.class));
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void importTickets_savesTicketsToRepository() {
        when(file.isEmpty()).thenReturn(false);
        when(csvParser.supports("csv")).thenReturn(true);
        
        Ticket ticket1 = new Ticket();
        Ticket ticket2 = new Ticket();
        List<Ticket> tickets = Arrays.asList(ticket1, ticket2);
        
        when(csvParser.parse(eq(file), any(ImportResult.class))).thenReturn(tickets);

        importService.importTickets(file, "csv");

        assertThat(ticketRepository.count()).isEqualTo(2);
    }
}

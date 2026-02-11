package com.support.ticketsystem.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.support.ticketsystem.dto.ImportResult;
import com.support.ticketsystem.exception.InvalidFileFormatException;
import com.support.ticketsystem.model.Ticket;
import com.support.ticketsystem.parser.TicketParser;
import com.support.ticketsystem.repository.TicketRepository;

@Service
public class ImportService {
    private final List<TicketParser> parsers;
    private final TicketRepository ticketRepository;

    public ImportService(List<TicketParser> parsers, TicketRepository ticketRepository) {
        this.parsers = parsers;
        this.ticketRepository = ticketRepository;
    }

    public ImportResult importTickets(MultipartFile file, String format) {
        if (file.isEmpty()) {
            throw new InvalidFileFormatException("File is empty");
        }

        TicketParser parser = parsers.stream()
                .filter(p -> p.supports(format))
                .findFirst()
                .orElseThrow(() -> new InvalidFileFormatException("Unsupported format: " + format));

        ImportResult result = new ImportResult();
        
        try {
            List<Ticket> tickets = parser.parse(file, result);
            
            // Save successfully parsed tickets
            for (Ticket ticket : tickets) {
                ticketRepository.save(ticket);
            }
            
        } catch (InvalidFileFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidFileFormatException("Error importing tickets: " + e.getMessage(), e);
        }

        return result;
    }
}

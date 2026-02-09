package com.support.ticketsystem.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.support.ticketsystem.dto.ImportResult;
import com.support.ticketsystem.exception.InvalidFileFormatException;
import com.support.ticketsystem.model.Ticket;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class XmlTicketParser implements TicketParser {
    private final XmlMapper xmlMapper;

    public XmlTicketParser() {
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public List<Ticket> parse(MultipartFile file, ImportResult result) {
        List<Ticket> tickets = new ArrayList<>();
        
        try {
            String xmlContent = new String(file.getInputStream().readAllBytes());
            
            // Handle both root element <tickets> and direct list
            List<Ticket> parsedTickets;
            if (xmlContent.contains("<tickets>")) {
                TicketsWrapper wrapper = xmlMapper.readValue(xmlContent, TicketsWrapper.class);
                parsedTickets = wrapper.getTickets();
            } else {
                parsedTickets = xmlMapper.readValue(
                        xmlContent,
                        new TypeReference<List<Ticket>>() {}
                );
            }
            
            result.setTotal(parsedTickets.size());
            
            for (int i = 0; i < parsedTickets.size(); i++) {
                Ticket ticket = parsedTickets.get(i);
                int rowNumber = i + 1;
                
                try {
                    validateTicket(ticket);
                    tickets.add(ticket);
                    result.setSuccessful(result.getSuccessful() + 1);
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    result.getErrors().add(new ImportResult.ImportError(rowNumber, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new InvalidFileFormatException("Failed to parse XML file: " + e.getMessage(), e);
        }
        
        return tickets;
    }

    private void validateTicket(Ticket ticket) {
        if (ticket.getCustomerId() == null || ticket.getCustomerId().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (ticket.getCustomerEmail() == null || ticket.getCustomerEmail().isBlank()) {
            throw new IllegalArgumentException("Customer email is required");
        }
        if (ticket.getSubject() == null || ticket.getSubject().isBlank()) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (ticket.getDescription() == null || ticket.getDescription().isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
    }

    @Override
    public boolean supports(String format) {
        return "xml".equalsIgnoreCase(format);
    }

    // Wrapper class for XML root element
    @com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement(localName = "tickets")
    private static class TicketsWrapper {
        @com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty(localName = "ticket")
        @com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper(useWrapping = false)
        private List<Ticket> tickets;

        public List<Ticket> getTickets() {
            return tickets;
        }

        public void setTickets(List<Ticket> tickets) {
            this.tickets = tickets;
        }
    }
}

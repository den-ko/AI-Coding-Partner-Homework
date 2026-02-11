package com.support.ticketsystem.parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.support.ticketsystem.dto.ImportResult;
import com.support.ticketsystem.exception.InvalidFileFormatException;
import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.DeviceType;
import com.support.ticketsystem.model.Metadata;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Source;
import com.support.ticketsystem.model.Status;
import com.support.ticketsystem.model.Ticket;

@Component
public class CsvTicketParser implements TicketParser {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public List<Ticket> parse(MultipartFile file, ImportResult result) {
        List<Ticket> tickets = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            
            if (rows.isEmpty()) {
                throw new InvalidFileFormatException("CSV file is empty");
            }

            // Skip header row
            String[] headers = rows.get(0);
            result.setTotal(rows.size() - 1);

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                int rowNumber = i + 1;
                
                try {
                    Ticket ticket = parseRow(row, headers);
                    tickets.add(ticket);
                    result.setSuccessful(result.getSuccessful() + 1);
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    result.getErrors().add(new ImportResult.ImportError(rowNumber, e.getMessage()));
                }
            }
        } catch (IOException | CsvException e) {
            throw new InvalidFileFormatException("Failed to parse CSV file: " + e.getMessage(), e);
        }
        
        return tickets;
    }

    private Ticket parseRow(String[] row, String[] headers) {
        Ticket ticket = new Ticket();
        
        for (int i = 0; i < headers.length && i < row.length; i++) {
            String header = headers[i].trim();
            String value = row[i].trim();
            
            try {
                switch (header) {
                    case "id":
                        if (!value.isEmpty()) {
                            ticket.setId(UUID.fromString(value));
                        }
                        break;
                    case "customer_id":
                        ticket.setCustomerId(value);
                        break;
                    case "customer_email":
                        ticket.setCustomerEmail(value);
                        break;
                    case "customer_name":
                        ticket.setCustomerName(value);
                        break;
                    case "subject":
                        ticket.setSubject(value);
                        break;
                    case "description":
                        ticket.setDescription(value);
                        break;
                    case "category":
                        ticket.setCategory(Category.valueOf(value.toUpperCase()));
                        break;
                    case "priority":
                        ticket.setPriority(Priority.valueOf(value.toUpperCase()));
                        break;
                    case "status":
                        ticket.setStatus(Status.valueOf(value.toUpperCase()));
                        break;
                    case "created_at":
                        if (!value.isEmpty()) {
                            ticket.setCreatedAt(LocalDateTime.parse(value, DATE_FORMATTER));
                        }
                        break;
                    case "updated_at":
                        if (!value.isEmpty()) {
                            ticket.setUpdatedAt(LocalDateTime.parse(value, DATE_FORMATTER));
                        }
                        break;
                    case "resolved_at":
                        if (!value.isEmpty()) {
                            ticket.setResolvedAt(LocalDateTime.parse(value, DATE_FORMATTER));
                        }
                        break;
                    case "assigned_to":
                        if (!value.isEmpty()) {
                            ticket.setAssignedTo(value);
                        }
                        break;
                    case "tags":
                        if (!value.isEmpty()) {
                            ticket.setTags(List.of(value.split(";")));
                        }
                        break;
                    case "source":
                        if (ticket.getMetadata() == null) {
                            ticket.setMetadata(new Metadata());
                        }
                        ticket.getMetadata().setSource(Source.valueOf(value.toUpperCase()));
                        break;
                    case "browser":
                        if (ticket.getMetadata() == null) {
                            ticket.setMetadata(new Metadata());
                        }
                        ticket.getMetadata().setBrowser(value);
                        break;
                    case "device_type":
                        if (ticket.getMetadata() == null) {
                            ticket.setMetadata(new Metadata());
                        }
                        ticket.getMetadata().setDeviceType(DeviceType.valueOf(value.toUpperCase()));
                        break;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error parsing field '" + header + "': " + e.getMessage());
            }
        }
        
        return ticket;
    }

    @Override
    public boolean supports(String format) {
        return "csv".equalsIgnoreCase(format);
    }
}

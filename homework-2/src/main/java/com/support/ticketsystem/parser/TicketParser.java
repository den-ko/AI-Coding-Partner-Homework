package com.support.ticketsystem.parser;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.support.ticketsystem.dto.ImportResult;
import com.support.ticketsystem.model.Ticket;

public interface TicketParser {
    List<Ticket> parse(MultipartFile file, ImportResult result);
    boolean supports(String format);
}

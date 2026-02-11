package com.support.ticketsystem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.support.ticketsystem.dto.ImportResult;
import com.support.ticketsystem.service.ImportService;

@RestController
@RequestMapping("/tickets")
public class ImportController {
    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResult> importTickets(
            @RequestParam("file") MultipartFile file,
            @RequestParam("format") String format) {
        
        ImportResult result = importService.importTickets(file, format);
        return ResponseEntity.ok(result);
    }
}

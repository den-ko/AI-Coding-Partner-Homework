package com.support.ticketsystem.controller;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.ticketsystem.repository.TicketRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    void autoClassifyTicket_existingTicket_appliesClassification() throws Exception {
        // Create a ticket first
        UUID ticketId = createSampleTicket("Cannot login to account", 
            "I am having authentication issues and cannot access my account");

        mockMvc.perform(post("/tickets/" + ticketId + "/auto-classify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ACCOUNT_ACCESS"))
                .andExpect(jsonPath("$.confidence").exists())
                .andExpect(jsonPath("$.reasoning").exists())
                .andExpect(jsonPath("$.keywordsFound").isArray());
    }

    @Test
    void autoClassifyTicket_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(post("/tickets/550e8400-e29b-41d4-a716-446655440000/auto-classify"))
                .andExpect(status().isNotFound());
    }

    @Test
    void autoClassifyTicket_billingKeywords_detectsBillingCategory() throws Exception {
        UUID ticketId = createSampleTicket("Billing issue", 
            "I was charged twice for my subscription and need a refund");

        mockMvc.perform(post("/tickets/" + ticketId + "/auto-classify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("BILLING_QUESTION"))
                .andExpect(jsonPath("$.keywordsFound").isArray());
    }

    @Test
    void autoClassifyTicket_urgentKeywords_detectsHighPriority() throws Exception {
        UUID ticketId = createSampleTicket("Critical security emergency", 
            "Urgent: this is a critical production issue causing system outage");

        mockMvc.perform(post("/tickets/" + ticketId + "/auto-classify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.confidence").exists());
    }

    @Test
    void autoClassifyTicket_updatesTicketWithClassification() throws Exception {
        UUID ticketId = createSampleTicket("Bug found with steps to reproduce", 
            "I found a defect: steps to reproduce - expected behavior differs from actual behavior");

        // Classify the ticket
        mockMvc.perform(post("/tickets/" + ticketId + "/auto-classify"))
                .andExpect(status().isOk());

        // Verify the ticket was updated with classification data
        mockMvc.perform(get("/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification_data").exists())
                .andExpect(jsonPath("$.classification_data.category").value("BUG_REPORT"))
                .andExpect(jsonPath("$.classification_data.auto_classified").value(true));
    }

    // Helper method to create sample tickets
    private UUID createSampleTicket(String subject, String description) throws Exception {
        String ticketJson = String.format("""
            {
              "customer_id": "CUST001",
              "customer_email": "test@example.com",
              "customer_name": "Test User",
              "subject": "%s",
              "description": "%s",
              "category": "OTHER",
              "priority": "MEDIUM",
              "status": "NEW",
              "tags": ["test"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """, subject, description);

        String response = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return UUID.fromString(node.get("id").asText());
    }
}

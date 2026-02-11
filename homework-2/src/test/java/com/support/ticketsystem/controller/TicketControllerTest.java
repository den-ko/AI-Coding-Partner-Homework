package com.support.ticketsystem.controller;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.ticketsystem.repository.TicketRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {

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
    void createTicket_Success() throws Exception {
        String ticketJson = """
            {
              "customer_id": "CUST001",
              "customer_email": "test@example.com",
              "customer_name": "Test User",
              "subject": "Test Subject",
              "description": "This is a test description with enough characters",
              "category": "TECHNICAL_ISSUE",
              "priority": "HIGH",
              "status": "NEW",
              "tags": ["test", "demo"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customer_email").value("test@example.com"))
                .andExpect(jsonPath("$.subject").value("Test Subject"));
    }

    @Test
    void createTicket_ValidationFailure() throws Exception {
        String invalidTicketJson = """
            {
              "customer_id": "CUST001",
              "customer_email": "invalid-email",
              "customer_name": "Test User",
              "subject": "",
              "description": "Short",
              "category": "TECHNICAL_ISSUE",
              "priority": "HIGH",
              "status": "NEW",
              "tags": [],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidTicketJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTickets_Success() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getTicketById_NotFound() throws Exception {
        mockMvc.perform(get("/tickets/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTicket_withAutoClassifyTrue_appliesClassification() throws Exception {
        String ticketJson = """
            {
              "customer_id": "CUST002",
              "customer_email": "user@example.com",
              "customer_name": "User Name",
              "subject": "Cannot login to my account",
              "description": "I am having authentication issues and cannot access my account",
              "category": "OTHER",
              "priority": "MEDIUM",
              "status": "NEW",
              "tags": ["login"],
              "metadata": {
                "source": "EMAIL",
                "browser": "Firefox",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(post("/tickets?autoClassify=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classification_data").exists())
                .andExpect(jsonPath("$.classification_data.category").value("ACCOUNT_ACCESS"))
                .andExpect(jsonPath("$.classification_data.auto_classified").value(true));
    }

    @Test
    void createTicket_withAutoClassifyFalse_noClassification() throws Exception {
        String ticketJson = """
            {
              "customer_id": "CUST003",
              "customer_email": "test@test.com",
              "customer_name": "Test Name",
              "subject": "Login problem",
              "description": "Cannot authenticate with my password",
              "category": "OTHER",
              "priority": "LOW",
              "status": "NEW",
              "tags": [],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Safari",
                "device_type": "MOBILE"
              }
            }
            """;

        mockMvc.perform(post("/tickets?autoClassify=false")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classification_data").doesNotExist());
    }

    @Test
    void getAllTickets_withCategoryFilter_returnsFilteredResults() throws Exception {
        // Create a ticket first
        createSampleTicket("BUG_REPORT");

        mockMvc.perform(get("/tickets?category=BUG_REPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].category").value("BUG_REPORT"));
    }

    @Test
    void getAllTickets_withPriorityFilter_returnsFilteredResults() throws Exception {
        createSampleTicket("TECHNICAL_ISSUE", "URGENT");

        mockMvc.perform(get("/tickets?priority=URGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].priority").value("URGENT"));
    }

    @Test
    void updateTicket_existingTicket_success() throws Exception {
        UUID ticketId = createSampleTicket("TECHNICAL_ISSUE");

        String updatedTicketJson = """
            {
              "customer_id": "CUST001",
              "customer_email": "updated@example.com",
              "customer_name": "Updated Name",
              "subject": "Updated Subject",
              "description": "This is an updated description with enough characters",
              "category": "TECHNICAL_ISSUE",
              "priority": "HIGH",
              "status": "IN_PROGRESS",
              "tags": ["updated"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(put("/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedTicketJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer_email").value("updated@example.com"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateTicket_nonExistent_returnsNotFound() throws Exception {
        String ticketJson = """
            {
              "customer_id": "CUST001",
              "customer_email": "test@example.com",
              "customer_name": "Test User",
              "subject": "Test Subject",
              "description": "This is a test description",
              "category": "TECHNICAL_ISSUE",
              "priority": "HIGH",
              "status": "NEW",
              "tags": [],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(put("/tickets/550e8400-e29b-41d4-a716-446655440000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTicket_existingTicket_success() throws Exception {
        UUID ticketId = createSampleTicket("TECHNICAL_ISSUE");

        mockMvc.perform(delete("/tickets/" + ticketId))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/tickets/" + ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTicket_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/tickets/550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(status().isNotFound());
    }

    // Helper method to create sample tickets for testing
    private UUID createSampleTicket(String category) throws Exception {
        return createSampleTicket(category, "MEDIUM");
    }

    private UUID createSampleTicket(String category, String priority) throws Exception {
        String ticketJson = String.format("""
            {
              "customer_id": "CUST001",
              "customer_email": "test@example.com",
              "customer_name": "Test User",
              "subject": "Test Subject",
              "description": "This is a test description with enough characters",
              "category": "%s",
              "priority": "%s",
              "status": "NEW",
              "tags": ["test"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """, category, priority);

        String response = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract ID from response using JSON parsing
        JsonNode node = objectMapper.readTree(response);
        return UUID.fromString(node.get("id").asText());
    }
}

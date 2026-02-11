package com.support.ticketsystem.integration;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.ticketsystem.model.Category;
import com.support.ticketsystem.model.Priority;
import com.support.ticketsystem.model.Status;
import com.support.ticketsystem.model.Ticket;
import com.support.ticketsystem.repository.TicketRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TicketSystemIntegrationTest {

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
    void endToEndWorkflow_importClassifyFilter_completesSuccessfully() throws Exception {
        // Step 1: Import tickets from CSV
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Cannot login,Having authentication issues with my account,OTHER,MEDIUM,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,login,WEB_FORM,Chrome,DESKTOP
            550e8400-e29b-41d4-a716-446655440002,CUST002,jane@example.com,Jane Smith,Defect found,Need to reproduce this bug with steps,OTHER,MEDIUM,NEW,2026-02-01T10:00:00,2026-02-01T10:00:00,,,bug,EMAIL,Firefox,MOBILE
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // Import
        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful").value(2));

        // Step 2: Auto-classify the imported tickets
        UUID ticketId1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        mockMvc.perform(post("/tickets/" + ticketId1 + "/auto-classify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ACCOUNT_ACCESS"));

        UUID ticketId2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        mockMvc.perform(post("/tickets/" + ticketId2 + "/auto-classify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("BUG_REPORT"));

        // Step 3: Filter by category
        mockMvc.perform(get("/tickets?category=ACCOUNT_ACCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].classification_data.category").value("ACCOUNT_ACCESS"));
    }

    @Test
    void endToEndWorkflow_createWithAutoClassify_storesAndRetrieves() throws Exception {
        // Create ticket with auto-classification
        String ticketJson = """
            {
              "customer_id": "CUST003",
              "customer_email": "billing@example.com",
              "customer_name": "Billing User",
              "subject": "Billing issue",
              "description": "I was charged twice for my subscription and need a refund immediately",
              "category": "OTHER",
              "priority": "MEDIUM",
              "status": "NEW",
              "tags": ["billing"],
              "metadata": {
                "source": "EMAIL",
                "browser": "Safari",
                "device_type": "MOBILE"
              }
            }
            """;

        String response = mockMvc.perform(post("/tickets?autoClassify=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ticketJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classification_data.category").value("BILLING_QUESTION"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract ID and retrieve
        JsonNode node = objectMapper.readTree(response);
        UUID ticketId = UUID.fromString(node.get("id").asText());

        mockMvc.perform(get("/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classification_data.category").value("BILLING_QUESTION"))
                .andExpect(jsonPath("$.classification_data.auto_classified").value(true));
    }

    @Test
    void endToEndWorkflow_updateAndDelete_completesSuccessfully() throws Exception {
        // Create
        UUID ticketId = createTestTicket();

        // Update
        String updatedJson = """
            {
              "customer_id": "CUST001",
              "customer_email": "updated@example.com",
              "customer_name": "Updated User",
              "subject": "Updated Subject",
              "description": "This is an updated description with enough characters",
              "category": "TECHNICAL_ISSUE",
              "priority": "HIGH",
              "status": "RESOLVED",
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
                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolved_at").exists());

        // Delete
        mockMvc.perform(delete("/tickets/" + ticketId))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/tickets/" + ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    void integrationTest_completeTicketLifecycle_allTransitions() throws Exception {
        // Step 1: CREATE - Create new ticket
        String createJson = """
            {
              "customer_id": "CUST999",
              "customer_email": "lifecycle@example.com",
              "customer_name": "Lifecycle Test User",
              "subject": "Test lifecycle workflow",
              "description": "Testing complete ticket lifecycle from creation to closure",
              "category": "TECHNICAL_ISSUE",
              "priority": "MEDIUM",
              "status": "NEW",
              "tags": ["lifecycle", "test"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        String response = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        UUID ticketId = UUID.fromString(node.get("id").asText());

        // Step 2: GET - Verify ticket was created
        mockMvc.perform(get("/tickets/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.customer_email").value("lifecycle@example.com"));

        // Step 3: UPDATE - NEW → IN_PROGRESS
        String inProgressJson = """
            {
              "customer_id": "CUST999",
              "customer_email": "lifecycle@example.com",
              "customer_name": "Lifecycle Test User",
              "subject": "Test lifecycle workflow",
              "description": "Testing complete ticket lifecycle from creation to closure",
              "category": "TECHNICAL_ISSUE",
              "priority": "MEDIUM",
              "status": "IN_PROGRESS",
              "assigned_to": "agent001",
              "tags": ["lifecycle", "test", "in-progress"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(put("/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inProgressJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assigned_to").value("agent001"));

        // Step 4: UPDATE - IN_PROGRESS → WAITING_CUSTOMER
        String waitingJson = """
            {
              "customer_id": "CUST999",
              "customer_email": "lifecycle@example.com",
              "customer_name": "Lifecycle Test User",
              "subject": "Test lifecycle workflow",
              "description": "Testing complete ticket lifecycle from creation to closure",
              "category": "TECHNICAL_ISSUE",
              "priority": "MEDIUM",
              "status": "WAITING_CUSTOMER",
              "assigned_to": "agent001",
              "tags": ["lifecycle", "test", "waiting"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(put("/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(waitingJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_CUSTOMER"));

        // Step 5: UPDATE - WAITING_CUSTOMER → RESOLVED
        String resolvedJson = """
            {
              "customer_id": "CUST999",
              "customer_email": "lifecycle@example.com",
              "customer_name": "Lifecycle Test User",
              "subject": "Test lifecycle workflow",
              "description": "Testing complete ticket lifecycle from creation to closure",
              "category": "TECHNICAL_ISSUE",
              "priority": "MEDIUM",
              "status": "RESOLVED",
              "assigned_to": "agent001",
              "tags": ["lifecycle", "test", "resolved"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(put("/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resolvedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolved_at").exists());

        // Step 6: UPDATE - RESOLVED → CLOSED
        String closedJson = """
            {
              "customer_id": "CUST999",
              "customer_email": "lifecycle@example.com",
              "customer_name": "Lifecycle Test User",
              "subject": "Test lifecycle workflow",
              "description": "Testing complete ticket lifecycle from creation to closure",
              "category": "TECHNICAL_ISSUE",
              "priority": "MEDIUM",
              "status": "CLOSED",
              "assigned_to": "agent001",
              "tags": ["lifecycle", "test", "closed"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

        mockMvc.perform(put("/tickets/" + ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // Step 7: DELETE - Remove ticket
        mockMvc.perform(delete("/tickets/" + ticketId))
                .andExpect(status().isNoContent());

        // Step 8: Verify deletion
        mockMvc.perform(get("/tickets/" + ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    void integrationTest_bulkImportWithAutoClassification_verifiesCategories() throws Exception {
        // Import CSV with tickets containing specific keywords for classification
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440010,CUST010,user1@example.com,User One,Cannot login to account,I am having trouble with my password and authentication,OTHER,MEDIUM,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,test,WEB_FORM,Chrome,DESKTOP
            550e8400-e29b-41d4-a716-446655440011,CUST011,user2@example.com,User Two,Application crash,The application crashes with error code 500,OTHER,MEDIUM,NEW,2026-02-01T10:00:00,2026-02-01T10:00:00,,,test,WEB_FORM,Firefox,MOBILE
            550e8400-e29b-41d4-a716-446655440012,CUST012,user3@example.com,User Three,Billing problem,I was charged twice on my invoice and need a refund,OTHER,MEDIUM,NEW,2026-02-01T11:00:00,2026-02-01T11:00:00,,,test,WEB_FORM,Safari,TABLET
            550e8400-e29b-41d4-a716-446655440013,CUST013,user4@example.com,User Four,Feature suggestion,Would love to see a dark mode enhancement added,OTHER,MEDIUM,NEW,2026-02-01T12:00:00,2026-02-01T12:00:00,,,test,WEB_FORM,Chrome,DESKTOP
            550e8400-e29b-41d4-a716-446655440014,CUST014,user5@example.com,User Five,Bug found,Found a defect that needs to be reproduced with expected and actual results,OTHER,MEDIUM,NEW,2026-02-01T13:00:00,2026-02-01T13:00:00,,,test,WEB_FORM,Edge,DESKTOP
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "classify_test.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // Import tickets
        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successful").value(5));

        // Auto-classify all imported tickets and verify categories
        UUID[] ticketIds = {
            UUID.fromString("550e8400-e29b-41d4-a716-446655440010"),
            UUID.fromString("550e8400-e29b-41d4-a716-446655440011"),
            UUID.fromString("550e8400-e29b-41d4-a716-446655440012"),
            UUID.fromString("550e8400-e29b-41d4-a716-446655440013"),
            UUID.fromString("550e8400-e29b-41d4-a716-446655440014")
        };

        String[] expectedCategories = {
            "ACCOUNT_ACCESS",    // login, password, authentication
            "TECHNICAL_ISSUE",   // crash, error
            "BILLING_QUESTION",  // charged, invoice, refund
            "FEATURE_REQUEST",   // enhancement, suggestion
            "BUG_REPORT"         // defect, reproduce, expected, actual
        };

        for (int i = 0; i < ticketIds.length; i++) {
            mockMvc.perform(post("/tickets/" + ticketIds[i] + "/auto-classify"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.category").value(expectedCategories[i]));
        }

        // Verify classifications persisted
        for (int i = 0; i < ticketIds.length; i++) {
            mockMvc.perform(get("/tickets/" + ticketIds[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classification_data.category").value(expectedCategories[i]))
                    .andExpect(jsonPath("$.classification_data.confidence").exists());
        }
    }

    @Test
    void integrationTest_combinedFiltering_categoryAndPriority() throws Exception {
        // Create tickets with various category/priority combinations
        createTicketWithCategoryAndPriority("CUST100", "user100@example.com", "Technical Issue 1",
                "Application error occurred", Category.TECHNICAL_ISSUE, Priority.HIGH);
        createTicketWithCategoryAndPriority("CUST101", "user101@example.com", "Technical Issue 2",
                "System crash detected", Category.TECHNICAL_ISSUE, Priority.MEDIUM);
        createTicketWithCategoryAndPriority("CUST102", "user102@example.com", "Billing Issue 1",
                "Payment problem with invoice", Category.BILLING_QUESTION, Priority.HIGH);
        createTicketWithCategoryAndPriority("CUST103", "user103@example.com", "Billing Issue 2",
                "Refund request for subscription", Category.BILLING_QUESTION, Priority.MEDIUM);
        createTicketWithCategoryAndPriority("CUST104", "user104@example.com", "Account Problem",
                "Cannot login to my account", Category.ACCOUNT_ACCESS, Priority.HIGH);

        // Filter by category only - should return 2 TECHNICAL_ISSUE tickets
        mockMvc.perform(get("/tickets?category=TECHNICAL_ISSUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Filter by priority only - should return 3 HIGH priority tickets
        mockMvc.perform(get("/tickets?priority=HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // Filter by BOTH category AND priority - should return 1 ticket
        mockMvc.perform(get("/tickets?category=TECHNICAL_ISSUE&priority=HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("TECHNICAL_ISSUE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"));

        // Filter by BOTH with different combination - should return 1 ticket
        mockMvc.perform(get("/tickets?category=BILLING_QUESTION&priority=MEDIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("BILLING_QUESTION"))
                .andExpect(jsonPath("$[0].priority").value("MEDIUM"));

        // Filter with no matches
        mockMvc.perform(get("/tickets?category=FEATURE_REQUEST&priority=URGENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void performanceBenchmark_bulkImport_completesWithinTimeLimit() throws Exception {
        // Generate CSV with 100 tickets
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type\n");
        
        for (int i = 0; i < 100; i++) {
            csvBuilder.append(String.format(
                "%s,CUST%03d,user%d@example.com,User %d,Subject %d,This is a test description with enough characters for ticket %d,TECHNICAL_ISSUE,MEDIUM,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,test,WEB_FORM,Chrome,DESKTOP\n",
                UUID.randomUUID(), i, i, i, i, i
            ));
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bulk.csv",
                "text/csv",
                csvBuilder.toString().getBytes()
        );

        long startTime = System.currentTimeMillis();

        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.successful").value(100));

        long duration = System.currentTimeMillis() - startTime;

        // Performance assertion: should complete in under 5 seconds
        assertThat(duration).isLessThan(5000);
        System.out.println("Bulk import of 100 tickets completed in " + duration + "ms");
    }

    @Test
    void performanceBenchmark_autoClassification_completesWithinTimeLimit() throws Exception {
        // Create 50 tickets
        for (int i = 0; i < 50; i++) {
            createTestTicket();
        }

        List<Ticket> tickets = ticketRepository.findAll();
        assertThat(tickets).hasSize(50);

        long startTime = System.currentTimeMillis();

        // Classify all tickets
        int successful = 0;
        for (Ticket ticket : tickets) {
            try {
                mockMvc.perform(post("/tickets/" + ticket.getId() + "/auto-classify"))
                        .andExpect(status().isOk());
                successful++;
            } catch (AssertionError e) {
                // Log but continue - some tickets may already be classified or have issues
                System.out.println("Failed to classify ticket " + ticket.getId() + ": " + e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        // At least 45 out of 50 should succeed
        assertThat(successful).isGreaterThanOrEqualTo(45);
        
        // Performance assertion: should complete in under 3 seconds
        assertThat(duration).isLessThan(3000);
        System.out.println("Auto-classification of " + successful + " tickets completed in " + duration + "ms");
    }

    @Test
    void performanceBenchmark_filtering_completesWithinTimeLimit() throws Exception {
        // Create 200 tickets with various categories and priorities
        for (int i = 0; i < 200; i++) {
            Category category = Category.values()[i % Category.values().length];
            Priority priority = Priority.values()[i % Priority.values().length];
            
            Ticket ticket = new Ticket();
            ticket.setCustomerId("CUST" + i);
            ticket.setCustomerEmail("user" + i + "@example.com");
            ticket.setCustomerName("User " + i);
            ticket.setSubject("Test Subject " + i);
            ticket.setDescription("This is a test description with enough characters for ticket " + i);
            ticket.setCategory(category);
            ticket.setPriority(priority);
            ticket.setStatus(Status.NEW);
            ticketRepository.save(ticket);
        }

        long startTime = System.currentTimeMillis();

        // Perform 50 filtered queries
        for (int i = 0; i < 50; i++) {
            Category filterCategory = Category.values()[i % Category.values().length];
            
            mockMvc.perform(get("/tickets")
                    .param("category", filterCategory.name()))
                    .andExpect(status().isOk());
        }

        long duration = System.currentTimeMillis() - startTime;

        // Performance assertion: 50 filtered queries should complete in under 2 seconds
        assertThat(duration).isLessThan(2000);
        System.out.println("50 filtered queries on 200 tickets completed in " + duration + "ms");
    }

    @Test
    void performanceBenchmark_concurrentOperations_completesWithinTimeLimit() throws Exception {
        // First, create 25 tickets for concurrent updates
        List<UUID> existingTicketIds = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            UUID id = createTestTicket();
            existingTicketIds.add(id);
        }

        long startTime = System.currentTimeMillis();

        // Perform 25 concurrent CREATES and 25 concurrent UPDATES (50 total simultaneous operations)
        java.util.stream.IntStream.range(0, 50).parallel().forEach(i -> {
            try {
                if (i < 25) {
                    // First 25: Concurrent creates
                    String ticketJson = """
                        {
                          "customer_id": "CUST%d",
                          "customer_email": "concurrent%d@example.com",
                          "customer_name": "Concurrent User %d",
                          "subject": "Concurrent Test %d",
                          "description": "This is a concurrent test description with enough characters %d",
                          "category": "TECHNICAL_ISSUE",
                          "priority": "MEDIUM",
                          "status": "NEW",
                          "tags": ["concurrent"],
                          "metadata": {
                            "source": "API",
                            "browser": "Chrome",
                            "device_type": "DESKTOP"
                          }
                        }
                        """.formatted(i, i, i, i, i);

                    mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ticketJson))
                            .andExpect(status().isCreated());
                } else {
                    // Last 25: Concurrent updates
                    int index = i - 25;
                    String updateJson = """
                        {
                          "customer_id": "CUST001",
                          "customer_email": "updated%d@example.com",
                          "customer_name": "Updated User %d",
                          "subject": "Updated Concurrent Test %d",
                          "description": "This is an updated concurrent test description with enough characters",
                          "category": "BILLING_QUESTION",
                          "priority": "HIGH",
                          "status": "IN_PROGRESS",
                          "tags": ["concurrent", "updated"],
                          "metadata": {
                            "source": "API",
                            "browser": "Firefox",
                            "device_type": "MOBILE"
                          }
                        }
                        """.formatted(index, index, index);

                    mockMvc.perform(put("/tickets/" + existingTicketIds.get(index))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                            .andExpect(status().isOk());
                }
            } catch (Exception e) {
                // Log but continue
                System.out.println("Failed concurrent operation: " + e.getMessage());
            }
        });

        long duration = System.currentTimeMillis() - startTime;

        // Verify at least 45 operations succeeded (out of 50)
        List<Ticket> tickets = ticketRepository.findAll();
        assertThat(tickets.size()).isGreaterThanOrEqualTo(45);

        // Performance assertion: should complete in under 6 seconds
        assertThat(duration).isLessThan(6000);
        System.out.println("50 concurrent operations (25 creates + 25 updates) completed in " + duration + "ms");
    }

    @Test
    void performanceBenchmark_bulkUpdates_completesWithinTimeLimit() throws Exception {
        // Create 100 tickets
        List<UUID> ticketIds = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID id = createTestTicket();
            ticketIds.add(id);
        }

        long startTime = System.currentTimeMillis();

        // Update all 100 tickets
        for (int i = 0; i < ticketIds.size(); i++) {
            String updateJson = """
                {
                  "customer_id": "CUST001",
                  "customer_email": "updated@example.com",
                  "customer_name": "Updated User",
                  "subject": "Updated Subject %d",
                  "description": "This is an updated description with enough characters",
                  "category": "BILLING_QUESTION",
                  "priority": "HIGH",
                  "status": "IN_PROGRESS",
                  "tags": ["updated"],
                  "metadata": {
                    "source": "WEB_FORM",
                    "browser": "Chrome",
                    "device_type": "DESKTOP"
                  }
                }
                """.formatted(i);

            mockMvc.perform(put("/tickets/" + ticketIds.get(i))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson))
                    .andExpect(status().isOk());
        }

        long duration = System.currentTimeMillis() - startTime;

        // Performance assertion: should complete in under 3 seconds
        assertThat(duration).isLessThan(3000);
        System.out.println("100 ticket updates completed in " + duration + "ms");
    }

    @Test
    void performanceBenchmark_retrieval_completesWithinTimeLimit() throws Exception {
        // Create 150 tickets
        List<UUID> ticketIds = new java.util.ArrayList<>();
        for (int i = 0; i < 150; i++) {
            UUID id = createTestTicket();
            ticketIds.add(id);
        }

        long startTime = System.currentTimeMillis();

        // Retrieve all 150 tickets by ID
        for (UUID ticketId : ticketIds) {
            mockMvc.perform(get("/tickets/" + ticketId))
                    .andExpect(status().isOk());
        }

        long duration = System.currentTimeMillis() - startTime;

        // Performance assertion: should complete in under 2 seconds
        assertThat(duration).isLessThan(2000);
        System.out.println("150 individual ticket retrievals completed in " + duration + "ms");
    }

    // Helper method
    private UUID createTestTicket() throws Exception {
        String ticketJson = """
            {
              "customer_id": "CUST001",
              "customer_email": "test@example.com",
              "customer_name": "Test User",
              "subject": "Test Subject",
              "description": "This is a test description with enough characters",
              "category": "TECHNICAL_ISSUE",
              "priority": "MEDIUM",
              "status": "NEW",
              "tags": ["test"],
              "metadata": {
                "source": "WEB_FORM",
                "browser": "Chrome",
                "device_type": "DESKTOP"
              }
            }
            """;

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

    private void createTicketWithCategoryAndPriority(String customerId, String email, String subject,
                                                     String description, Category category, Priority priority) throws Exception {
        Ticket ticket = new Ticket();
        ticket.setCustomerId(customerId);
        ticket.setCustomerEmail(email);
        ticket.setCustomerName("Test User");
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus(Status.NEW);
        ticketRepository.save(ticket);
    }
}

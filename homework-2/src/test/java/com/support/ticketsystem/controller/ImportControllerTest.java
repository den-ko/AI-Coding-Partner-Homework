package com.support.ticketsystem.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.support.ticketsystem.repository.TicketRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    void importTickets_csvFormat_success() throws Exception {
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Login Issue,Cannot access my account details,ACCOUNT_ACCESS,HIGH,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,login;urgent,WEB_FORM,Chrome,DESKTOP
            550e8400-e29b-41d4-a716-446655440002,CUST002,jane@example.com,Jane Smith,Billing Question,Need help with invoice,BILLING_QUESTION,MEDIUM,NEW,2026-02-01T10:00:00,2026-02-01T10:00:00,,,billing,EMAIL,Firefox,MOBILE
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_tickets.csv",
                "text/csv",
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.successful").value(2))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    void importTickets_jsonFormat_success() throws Exception {
        String jsonContent = """
            [
              {
                "customer_id": "CUST001",
                "customer_email": "test@example.com",
                "customer_name": "Test User",
                "subject": "Test Subject",
                "description": "This is a test description with enough characters",
                "category": "TECHNICAL_ISSUE",
                "priority": "HIGH",
                "status": "NEW",
                "tags": ["test"],
                "metadata": {
                  "source": "WEB_FORM",
                  "browser": "Chrome",
                  "device_type": "DESKTOP"
                }
              }
            ]
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_tickets.json",
                "application/json",
                jsonContent.getBytes()
        );

        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.successful").value(1))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    void importTickets_xmlFormat_success() throws Exception {
        String xmlContent = """
            <tickets>
              <ticket>
                <customer_id>CUST001</customer_id>
                <customer_email>test@example.com</customer_email>
                <customer_name>Test User</customer_name>
                <subject>Test Subject</subject>
                <description>This is a test description with enough characters</description>
                <category>TECHNICAL_ISSUE</category>
                <priority>HIGH</priority>
                <status>NEW</status>
                <tags>
                  <tag>test</tag>
                </tags>
                <metadata>
                  <source>WEB_FORM</source>
                  <browser>Chrome</browser>
                  <device_type>DESKTOP</device_type>
                </metadata>
              </ticket>
            </tickets>
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_tickets.xml",
                "application/xml",
                xmlContent.getBytes()
        );

        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "xml"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.successful").value(1))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    void importTickets_emptyFile_throwsException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "csv"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importTickets_unsupportedFormat_throwsException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "some content".getBytes()
        );

        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "txt"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importTickets_partialErrors_returnsErrorDetails() throws Exception {
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Login Issue,Cannot access my account details,ACCOUNT_ACCESS,HIGH,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,login,WEB_FORM,Chrome,DESKTOP
            550e8400-e29b-41d4-a716-446655440002,CUST002,invalid-email,Jane Smith,Billing,Short description,BILLING_QUESTION,INVALID_PRIORITY,NEW,2026-02-01T10:00:00,2026-02-01T10:00:00,,,billing,EMAIL,Firefox,MOBILE
            550e8400-e29b-41d4-a716-446655440003,CUST003,valid@example.com,Valid User,Valid Subject,This is a valid description with enough characters,FEATURE_REQUEST,LOW,NEW,2026-02-01T11:00:00,2026-02-01T11:00:00,,,feature,API,Safari,TABLET
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mixed_tickets.csv",
                "text/csv",
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/tickets/import")
                .file(file)
                .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.successful").value(2))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].row").value(3));
    }
}

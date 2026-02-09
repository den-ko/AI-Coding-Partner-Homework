package com.support.ticketsystem.parser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.support.ticketsystem.dto.ImportResult;
import com.support.ticketsystem.exception.InvalidFileFormatException;
import com.support.ticketsystem.model.Ticket;

class JsonTicketParserTest {

    private JsonTicketParser jsonParser;

    @BeforeEach
    void setUp() {
        jsonParser = new JsonTicketParser();
    }

    @Test
    void parse_validJsonFile_returnsTickets() {
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
                "tags": ["test", "demo"],
                "metadata": {
                  "source": "WEB_FORM",
                  "browser": "Chrome",
                  "device_type": "DESKTOP"
                }
              }
            ]
            """;

        MultipartFile file = createMockFile("test.json", jsonContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = jsonParser.parse(file, result);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getCustomerEmail()).isEqualTo("test@example.com");
        assertThat(tickets.get(0).getTags()).contains("test", "demo");
    }

    @Test
    void parse_emptyJsonArray_returnsEmptyList() {
        String jsonContent = "[]";

        MultipartFile file = createMockFile("empty.json", jsonContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = jsonParser.parse(file, result);

        assertThat(tickets).isEmpty();
    }

    @Test
    void parse_invalidJsonSyntax_throwsException() {
        String jsonContent = "{ invalid json }";

        MultipartFile file = createMockFile("invalid.json", jsonContent);
        ImportResult result = new ImportResult();

        assertThatThrownBy(() -> jsonParser.parse(file, result))
                .isInstanceOf(InvalidFileFormatException.class);
    }

    @Test
    void parse_missingRequiredFields_recordsErrors() {
        String jsonContent = """
            [
              {
                "customer_id": "CUST001",
                "customer_email": "test@example.com",
                "customer_name": "",
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
            ]
            """;

        MultipartFile file = createMockFile("invalid.json", jsonContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = jsonParser.parse(file, result);

        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void supports_jsonFormat_returnsTrue() {
        assertThat(jsonParser.supports("json")).isTrue();
    }

    @Test
    void supports_nonJsonFormat_returnsFalse() {
        assertThat(jsonParser.supports("csv")).isFalse();
        assertThat(jsonParser.supports("xml")).isFalse();
    }

    // Helper method
    private MockMultipartFile createMockFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/json",
                content.getBytes()
        );
    }
}

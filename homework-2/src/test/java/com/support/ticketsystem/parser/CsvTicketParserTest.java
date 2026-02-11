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

class CsvTicketParserTest {

    private CsvTicketParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvTicketParser();
    }

    @Test
    void parse_validCsvFile_returnsTickets() {
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Login Issue,Cannot access my account details,ACCOUNT_ACCESS,HIGH,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,login;urgent,WEB_FORM,Chrome,DESKTOP
            550e8400-e29b-41d4-a716-446655440002,CUST002,jane@example.com,Jane Smith,Billing Question,Need help with my invoice and payment,BILLING_QUESTION,MEDIUM,NEW,2026-02-01T10:00:00,2026-02-01T10:00:00,,,billing,EMAIL,Firefox,MOBILE
            """;

        MultipartFile file = createMockFile("test.csv", csvContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = csvParser.parse(file, result);

        assertThat(tickets).hasSize(2);
        assertThat(tickets.get(0).getCustomerEmail()).isEqualTo("john@example.com");
        assertThat(tickets.get(0).getTags()).contains("login", "urgent");
        assertThat(tickets.get(1).getCustomerEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void parse_emptyFile_throwsException() {
        MultipartFile file = createMockFile("empty.csv", "");
        ImportResult result = new ImportResult();

        assertThatThrownBy(() -> csvParser.parse(file, result))
                .isInstanceOf(InvalidFileFormatException.class);
    }

    @Test
    void parse_invalidEnumValue_recordsError() {
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Test Subject,This is a test description,INVALID_CATEGORY,HIGH,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,test,WEB_FORM,Chrome,DESKTOP
            """;

        MultipartFile file = createMockFile("invalid.csv", csvContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = csvParser.parse(file, result);

        assertThat(tickets).isEmpty();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void parse_invalidDateFormat_recordsError() {
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Test Subject,This is a test description,TECHNICAL_ISSUE,HIGH,NEW,invalid-date,2026-02-01T09:00:00,,,test,WEB_FORM,Chrome,DESKTOP
            """;

        MultipartFile file = createMockFile("invalid_date.csv", csvContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = csvParser.parse(file, result);

        assertThat(tickets).isEmpty();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void parse_semicolonSeparatedTags_parsesCorrectly() {
        String csvContent = """
            id,customer_id,customer_email,customer_name,subject,description,category,priority,status,created_at,updated_at,resolved_at,assigned_to,tags,source,browser,device_type
            550e8400-e29b-41d4-a716-446655440001,CUST001,john@example.com,John Doe,Test Subject,This is a test description,TECHNICAL_ISSUE,HIGH,NEW,2026-02-01T09:00:00,2026-02-01T09:00:00,,,tag1;tag2;tag3,WEB_FORM,Chrome,DESKTOP
            """;

        MultipartFile file = createMockFile("tags.csv", csvContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = csvParser.parse(file, result);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getTags()).containsExactly("tag1", "tag2", "tag3");
    }

    @Test
    void supports_csvFormat_returnsTrue() {
        assertThat(csvParser.supports("csv")).isTrue();
    }

    @Test
    void supports_nonCsvFormat_returnsFalse() {
        assertThat(csvParser.supports("json")).isFalse();
        assertThat(csvParser.supports("xml")).isFalse();
    }

    // Helper method
    private MockMultipartFile createMockFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes()
        );
    }
}

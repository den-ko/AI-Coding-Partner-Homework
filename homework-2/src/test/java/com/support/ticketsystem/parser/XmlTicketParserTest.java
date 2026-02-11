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

class XmlTicketParserTest {

    private XmlTicketParser xmlParser;

    @BeforeEach
    void setUp() {
        xmlParser = new XmlTicketParser();
    }

    @Test
    void parse_validXmlWithWrapper_returnsTickets() {
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
                  <tag>demo</tag>
                </tags>
                <metadata>
                  <source>WEB_FORM</source>
                  <browser>Chrome</browser>
                  <device_type>DESKTOP</device_type>
                </metadata>
              </ticket>
            </tickets>
            """;

        MultipartFile file = createMockFile("test.xml", xmlContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = xmlParser.parse(file, result);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getCustomerEmail()).isEqualTo("test@example.com");
        assertThat(tickets.get(0).getTags()).contains("test", "demo");
    }

    @Test
    void parse_invalidXmlSyntax_throwsException() {
        String xmlContent = "<invalid><unclosed>";

        MultipartFile file = createMockFile("invalid.xml", xmlContent);
        ImportResult result = new ImportResult();

        assertThatThrownBy(() -> xmlParser.parse(file, result))
                .isInstanceOf(InvalidFileFormatException.class);
    }

    @Test
    void parse_missingRequiredFields_recordsErrors() {
        String xmlContent = """
            <tickets>
              <ticket>
                <customer_id>CUST001</customer_id>
                <customer_email>test@example.com</customer_email>
                <customer_name></customer_name>
                <subject></subject>
                <description>Short</description>
                <category>TECHNICAL_ISSUE</category>
                <priority>HIGH</priority>
                <status>NEW</status>
                <tags></tags>
                <metadata>
                  <source>WEB_FORM</source>
                  <device_type>DESKTOP</device_type>
                </metadata>
              </ticket>
            </tickets>
            """;

        MultipartFile file = createMockFile("invalid.xml", xmlContent);
        ImportResult result = new ImportResult();

        List<Ticket> tickets = xmlParser.parse(file, result);

        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    void supports_xmlFormat_returnsTrue() {
        assertThat(xmlParser.supports("xml")).isTrue();
    }

    @Test
    void supports_nonXmlFormat_returnsFalse() {
        assertThat(xmlParser.supports("csv")).isFalse();
        assertThat(xmlParser.supports("json")).isFalse();
    }

    // Helper method
    private MockMultipartFile createMockFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/xml",
                content.getBytes()
        );
    }
}

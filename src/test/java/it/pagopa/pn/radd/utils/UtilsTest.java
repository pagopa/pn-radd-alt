package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.DownloadUrl;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
    /**
     * Method under test: {@link Utils#checkOperationType(String)}
     */
    @Test
    void testCheckOperationType() {
        assertFalse(Utils.checkOperationType("Operation Type"));
    }

    /**
     * Method under test: {@link Utils#checkPersonType(String)}
     */
    @Test
    void testCheckPersonType() {
        assertFalse(Utils.checkPersonType("Person Type"));
        assertTrue(Utils.checkPersonType(Const.PF));
        assertTrue(Utils.checkPersonType(Const.PG));
    }

    /**
     * Method under test: {@link Utils#getDocumentDownloadUrl(String, String, String, String, String)}
     */
    @Test
    void testGetDocumentDownloadUrl() {
        DownloadUrl actualDocumentDownloadUrl = Utils.getDocumentDownloadUrl("https://example.org/example",
                "https://example.org/example", "https://example.org/example", "https://example.org/example", "DOCUMENT");
        assertTrue(actualDocumentDownloadUrl.getNeedAuthentication());
        assertEquals(
                "https://example.org/example/radd-net/api/v1/download/https://example.org/example/https://example.org"
                        + "/example?attachmentId=https://example.org/example",
                actualDocumentDownloadUrl.getUrl());
    }

    /**
     * Method under test: {@link Utils#getDocumentDownloadUrl(String, String, String, String, String)}
     */
    @Test
    void testGetDocumentDownloadUrl4() {
        DownloadUrl actualDocumentDownloadUrl = Utils.getDocumentDownloadUrl("https://example.org/example",
                "https://example.org/example", "https://example.org/example", null, "DOCUMENT");
        assertTrue(actualDocumentDownloadUrl.getNeedAuthentication());
        assertEquals(
                "https://example.org/example/radd-net/api/v1/download/https://example.org/example/https://example.org"
                        + "/example",
                actualDocumentDownloadUrl.getUrl());
    }

    /**
     * Method under test: {@link Utils#transactionIdBuilder(CxTypeAuthFleet, String, String)}
     */
    @Test
    void testTransactionIdBuilder() {
        assertEquals("PA#42#42", Utils.transactionIdBuilder(CxTypeAuthFleet.PA, "42", "42"));
        assertEquals("PF#42#42", Utils.transactionIdBuilder(CxTypeAuthFleet.PF, "42", "42"));
        assertEquals("PG#42#42", Utils.transactionIdBuilder(CxTypeAuthFleet.PG, "42", "42"));
    }

    @Test
    void generateUUIDFromString_validInput_shouldGenerateUUID() {
        String input = "testInput";
        String uuid = Utils.generateUUIDFromString(input);

        assertNotNull(uuid);
        assertDoesNotThrow(() -> UUID.fromString(uuid)); // verifica formato UUID valido
    }

    @Test
    void getFileKeyFromPresignedUrl_shouldExtractFromSafestorageUrl() {
        String url = "https://example.safestorage/file-key-123?token=abc";
        String key = Utils.getFileKeyFromPresignedUrl(url);

        assertEquals("file-key-123", key);
    }

    @Test
    void getFileKeyFromPresignedUrl_shouldReturnZipUrl() {
        String url = "https://host/download/ACT/abc?attachmentId=123";
        String key = Utils.getFileKeyFromPresignedUrl(url);

        assertEquals("zipUrl", key);
    }

    @Test
    void getFileKeyFromPresignedUrl_shouldReturnCoverFileUrl() {
        String url = "https://host/download/AOR/abc";
        String key = Utils.getFileKeyFromPresignedUrl(url);

        assertEquals("coverFileUrl", key);
    }

    @Test
    void getFileKeyFromPresignedUrl_shouldReturnEmptyStringIfNoMatch() {
        String url = "https://host/unknown";
        String key = Utils.getFileKeyFromPresignedUrl(url);

        assertEquals("", key);
    }

    @Test
    void matchRegex_shouldNotThrow_whenMatchIsValid() {
        assertDoesNotThrow(() -> Utils.matchRegex("[0-9]{3}", "123", ExceptionTypeEnum.GENERIC_ERROR));
    }

}


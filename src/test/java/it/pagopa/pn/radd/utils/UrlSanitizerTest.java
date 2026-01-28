package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.exception.UrlSanitizeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UrlSanitizerTest {

    @ParameterizedTest
    @MethodSource("provideSanitizeUrlValidCases")
    void sanitizeUrl_withValidInputs_shouldReturnExpectedOutput(String input, String expected) {
        String result = UrlSanitizer.sanitizeUrl(input);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideSanitizeUrlValidCases() {
        return Stream.of(
                Arguments.of("https://example.com/test", "https://example.com/test"),
                Arguments.of("example.com/test", "https://example.com/test"),
                Arguments.of("https://example.com:8443/test", "https://example.com:8443/test"),
                Arguments.of("example.com/test?foo=bar", "https://example.com/test?foo=bar")
        );
    }

    @Test
    void sanitizeUrl_NullInput_shouldReturnNull() {
        assertNull(UrlSanitizer.sanitizeUrl(null));
    }

    @Test
    void sanitizeUrl_withInvalidUriSyntax_shouldThrowException() {
        UrlSanitizeException ex = assertThrows(
                UrlSanitizeException.class,
                () -> UrlSanitizer.sanitizeUrl("https://")
        );
        assertTrue(ex.getMessage().contains("Errore nella sanitizzazione"));
    }

    @Test
    void sanitizeUrl_withoutTLD_shouldThrowException() {
        UrlSanitizeException ex = assertThrows(
                UrlSanitizeException.class,
                () -> UrlSanitizer.sanitizeUrl("www.esempio")
        );
        assertTrue(ex.getMessage().contains("TLD non valido"));
    }
    @Test
    void validateUrl_withNull_shouldThrowException() {
        UrlSanitizeException ex = assertThrows(
                UrlSanitizeException.class,
                () -> UrlSanitizer.validateUrl(null)
                                              );
        assertTrue(ex.getMessage().contains("vuoto o nullo"));
    }

    @Test
    void validateUrl_withEmptyString_shouldThrowException() {
        UrlSanitizeException ex = assertThrows(
                UrlSanitizeException.class,
                () -> UrlSanitizer.validateUrl("   ")
                                              );
        assertTrue(ex.getMessage().contains("vuoto o nullo"));
    }

    @Test
    void validateUrl_withDangerousCharacters_shouldThrowException() {
        UrlSanitizeException ex = assertThrows(
                UrlSanitizeException.class,
                () -> UrlSanitizer.validateUrl("example.com/<script>")
                                              );
        assertTrue(ex.getMessage().contains("caratteri non validi"));
    }

    @Test
    void validateUrl_withUnsupportedProtocol_shouldThrowException() {
        UrlSanitizeException ex = assertThrows(
                UrlSanitizeException.class,
                () -> UrlSanitizer.validateUrl("http://example.com")
                                              );
        assertTrue(ex.getMessage().contains("Protocollo non supportato"));
    }

}

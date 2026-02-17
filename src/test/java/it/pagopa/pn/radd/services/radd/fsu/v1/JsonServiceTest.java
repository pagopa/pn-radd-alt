package it.pagopa.pn.radd.services.radd.fsu.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonService Tests")
@CustomLog
class JsonServiceTest {

    private JsonService jsonService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestPojo {
        private String name;
        private Integer age;
        private Boolean active;
    }

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        jsonService = new JsonService(objectMapper);
    }

    @Test
    @DisplayName("Should successfully parse valid JSON to POJO")
    void parseValidJsonToPojo() {
        // Arrange
        String jsonString = "{\"name\":\"John\",\"age\":30,\"active\":true}";

        // Act
        TestPojo result = jsonService.parse(jsonString, TestPojo.class);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals(30, result.getAge());
        assertTrue(result.getActive());
    }

    @Test
    @DisplayName("Should throw PnInternalException when JsonProcessingException occurs")
    void throwPnInternalExceptionOnJsonProcessingException() {
        // Arrange
        String invalidJson = "{invalid json}";

        // Act & Assert
        PnInternalException exception = assertThrows(
            PnInternalException.class,
            () -> jsonService.parse(invalidJson, TestPojo.class)
        );
        assertEquals("Couldn't parse JSON string to the desired class", exception.getProblem().getDetail());
        assertEquals("JSON_PARSE", exception.getProblem().getErrors().get(0).getCode());
    }

    @Test
    @DisplayName("Should throw PnInternalException for null JSON string")
    void throwExceptionForNullString() {
        // Act & Assert
        assertThrows(
            IllegalArgumentException.class,
            () -> jsonService.parse(null, TestPojo.class)
        );
    }
}

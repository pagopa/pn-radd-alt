package it.pagopa.pn.radd.services.radd.fsu.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JsonService {
    private final ObjectMapper objectMapper;

    public <T> T parse(String str, Class<T> clazz) {
        try {
            return objectMapper.readValue(str, clazz);
        } catch (JsonProcessingException e) {
            throw new PnInternalException("Couldn't parse JSON string to the desired class", "JSON_PARSE");
        }
    }

}

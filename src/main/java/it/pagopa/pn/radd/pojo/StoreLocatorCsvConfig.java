package it.pagopa.pn.radd.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreLocatorCsvConfig {

    private String header;
    private String field;
    private String value;
}

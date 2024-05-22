package it.pagopa.pn.radd.pojo;

import lombok.Data;

import java.util.List;

@Data
public class StoreLocatorConfiguration {
    private String version;
    private List<StoreLocatorCsvConfig> configs;
}

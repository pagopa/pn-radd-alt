package it.pagopa.pn.radd.pojo;

import lombok.Data;

import java.time.Instant;

@Data
public class RaddRegistryOriginalRequest {
    private String addressRow;
    private String cap;
    private String city;
    private String pr;
    private String country;
    private Instant startValidity;
    private Instant endValidity;
    private String openingTime;
    private String description;
    private String geoLocation;
    private String phoneNumber;
}
package it.pagopa.pn.radd.pojo;

import lombok.Data;

import java.util.List;
@Data
public class NormalizationRequest {

    private String correlationId;
    private List<NormalizationRequestAddress> addresses;
}

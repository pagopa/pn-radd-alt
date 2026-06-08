package it.pagopa.pn.radd.services.radd.fsu.v1.validation;

import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.CreateRegistryRequestV2;
import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.SelectiveUpdateRegistryRequestV2;
import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.UpdateRegistryRequestV2;
import org.springframework.stereotype.Component;

@Component
public class RegistryValidatorDispatcher {

    public void validate(CreateRegistryRequestV2 req) {
        BaseValidator.builder()
                .withOpeningTime(req.getOpeningTime())
                .withDateRange(req.getStartValidity(), req.getEndValidity())
                .withWebsite(req.getWebsite())
                .validate();
    }

    public void validate(UpdateRegistryRequestV2 req) {
        BaseValidator.builder()
                .withOpeningTime(req.getOpeningTime())
                .withWebsite(req.getWebsite())
                .validate();
    }

    public void validate(SelectiveUpdateRegistryRequestV2 req) {
        BaseValidator.builder()
                .withOpeningTime(req.getOpeningTime())
                .withDateRange(req.getStartValidity(), req.getEndValidity())
                .withWebsite(req.getWebsite())
                .validate();
    }

}
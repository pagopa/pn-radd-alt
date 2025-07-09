package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;

@ContextConfiguration(classes = {RegistrySelfController.class, RestExceptionHandler.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {RegistrySelfController.class})
class RegistrySelfControllerTest {

    @MockBean
    private RegistrySelfService registrySelfService;

    @Autowired
    WebTestClient webTestClient;

    private final String PARTNER_ID = "partnerId";
    private final String LOCATION_ID = "locationId";

    private static final String PATTERN_FORMAT = "yyyy-MM-dd";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

    private final String UPDATE_PATH = "/radd-net/api/v2/registry/{partnerId}/{locationId}";

    private UpdateRegistryRequestV2 buildValidRequest() {
        UpdateRegistryRequestV2 request = new UpdateRegistryRequestV2();

        Instant now = Instant.now();
        formatter.format(now);
        request.setEndValidity(formatter.format(now.plus(1, ChronoUnit.DAYS)));
        request.setDescription("description");
        request.setPhoneNumbers(List.of("+390123456789"));
        request.setExternalCodes(List.of("EXT0"));
        request.setEmail("mail@esempio.it");
        //request.setOpeningTime("mon=10:00-13:00_14:00-20:00#tue=10:00-20:00");
        request.setAppointmentRequired(true);
        request.setWebsite("https://test.it");
        return request;
    }

    @Test
    void updateRegistry_success() {
        UpdateRegistryRequestV2 request = buildValidRequest();

        RegistryV2 response = new RegistryV2();
        response.setPartnerId(PARTNER_ID);
        response.setLocationId(LOCATION_ID);

        Mockito.when(registrySelfService.updateRegistry(eq(PARTNER_ID), eq(LOCATION_ID), any()))
               .thenReturn(Mono.just(response));

        webTestClient.patch()
                     .uri(UPDATE_PATH, PARTNER_ID,LOCATION_ID)
                     .header("x-pagopa-pn-cx-type", "RADD")
                     .header("x-pagopa-pn-cx-id", "test-cx-id")
                     .header("uid", "test-uid")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(request)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectBody()
                     .jsonPath("$.partnerId").isEqualTo(PARTNER_ID)
                     .jsonPath("$.locationId").isEqualTo(LOCATION_ID);
    }

    @Test
    void updateRegistry_invalidField() {
        UpdateRegistryRequestV2 request = buildValidRequest();
        request.setWebsite("not-a-website");

        webTestClient.patch()
                     .uri(UPDATE_PATH, PARTNER_ID, LOCATION_ID)
                     .header("x-pagopa-pn-cx-type", "RADD")
                     .header("x-pagopa-pn-cx-id", "test-cx-id")
                     .header("uid", "test-uid")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(request)
                     .exchange()
                     .expectStatus()
                     .isBadRequest();

    }

}
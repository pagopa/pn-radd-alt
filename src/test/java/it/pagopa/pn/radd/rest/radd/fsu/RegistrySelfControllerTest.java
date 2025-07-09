package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import lombok.CustomLog;
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

    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_UID = "uid";
    public static final String LIMIT = "limit";
    public static final String LASTKEY = "lastKey";
    public static final String CAP = "cap";
    public static final String CITY = "city";
    public static final String PR = "pr";
    public static final String EXTERNALCODE = "externalCode";
    public static final String PARTNER_ID = "12345678901";

    private final String CREATE_PATH = "/radd-net/api/v2/registry/{partnerId}";

    private CreateRegistryRequestV2 buildValidRequest() {
        AddressV2 address = new AddressV2();
        address.setAddressRow("Via Roma 123");
        address.setCap("00100");
        address.setCity("Roma");
        address.setProvince("RM");
        address.setCountry("Italia");

        CreateRegistryRequestV2 req = new CreateRegistryRequestV2();
        req.setAddress(address);
        req.setPartnerId(PARTNER_ID);
        req.setLocationId("loc-1");
        req.setDescription("Sportello Test");
        req.setPhoneNumbers(List.of("+390123456789"));
        req.setExternalCodes(List.of("EXT1"));
        req.setEmail("mail@esempio.it");
        req.setCapacity("100");
        req.setOpeningTime("mon=10:00-13:00_14:00-20:00#tue=10:00-20:00");
        req.setStartValidity("2024-03-21");
        req.setEndValidity("2024-03-22");
        req.setAppointmentRequired(true);
        req.setWebsite("https://test.it");
        req.setPartnerType("CAF");
        return req;
    }

    @Test
    void addRegistry_success() {
        CreateRegistryRequestV2 request = buildValidRequest();
        RegistryV2 response = new RegistryV2();
        response.setPartnerId(PARTNER_ID);
        response.setLocationId(request.getLocationId());

        Mockito.when(registrySelfService.addRegistry(eq(PARTNER_ID), eq(request.getLocationId()), anyString(), any()))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(CREATE_PATH, PARTNER_ID)
                .header("x-pagopa-pn-cx-type", "RADD")
                .header("x-pagopa-pn-cx-id", "test-cx-id")
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.partnerId").isEqualTo(PARTNER_ID)
                .jsonPath("$.locationId").isEqualTo(request.getLocationId());
    }

    @Test
    void addRegistry_missingRequiredField() {
        CreateRegistryRequestV2 request = buildValidRequest();
        request.setAddress(null);

        webTestClient.post()
                .uri(CREATE_PATH, PARTNER_ID)
                .header("x-pagopa-pn-cx-type", "RADD")
                .header("x-pagopa-pn-cx-id", "test-cx-id")
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .value(problem -> Assertions.assertTrue(problem.getTitle().contains("non deve essere null")));
    }

    @Test
    void addRegistry_invalidField() {
        CreateRegistryRequestV2 request = buildValidRequest();
        request.setEmail("not-an-email");

        webTestClient.post()
                .uri(CREATE_PATH, PARTNER_ID)
                .header("x-pagopa-pn-cx-type", "RADD")
                .header("x-pagopa-pn-cx-id", "test-cx-id")
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(Problem.class)
                .value(problem -> Assertions.assertTrue(problem.getTitle().contains("deve corrispondere a")));
    }

}
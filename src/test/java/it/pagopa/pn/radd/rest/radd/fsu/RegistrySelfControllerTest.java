package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.exception.CoordinatesNotFoundException;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ContextConfiguration(classes = {RegistrySelfController.class, RestExceptionHandler.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {RegistrySelfController.class})
class RegistrySelfControllerTest {

    @MockBean
    private RegistrySelfService registrySelfService;

    @Autowired
    WebTestClient webTestClient;

    private final String PARTNER_ID = "12345678901";
    private final String LOCATION_ID = "locationId";
    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_UID = "uid";
    public static final String LAST_KEY = "lastKey";

    private final String CREATE_PATH = "/radd-bo/api/v2/registry";
    private final String UPDATE_PATH = "/radd-bo/api/v2/registry/{locationId}";

    private static final String PATTERN_FORMAT = "yyyy-MM-dd";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

    private CreateRegistryRequestV2 buildValidCreateRequest() {
        AddressV2 address = new AddressV2();
        address.setAddressRow("Via Roma 123");
        address.setCap("00100");
        address.setCity("Roma");
        address.setProvince("RM");
        address.setCountry("Italia");

        CreateRegistryRequestV2 req = new CreateRegistryRequestV2();
        req.setAddress(address);
        req.setDescription("Sportello Test");
        req.setPhoneNumbers(List.of("+390123456789"));
        req.setExternalCodes(List.of("EXT1"));
        req.setEmail("mail@esempio.it");
        req.setOpeningTime("mon=10:00-13:00_14:00-20:00#tue=10:00-20:00");
        req.setStartValidity("2024-03-21");
        req.setEndValidity("2024-03-22");
        req.setAppointmentRequired(true);
        req.setWebsite("https://test.it");
        req.setPartnerType("CAF");
        return req;
    }

    private UpdateRegistryRequestV2 buildValidUpdateRequest() {
        UpdateRegistryRequestV2 request = new UpdateRegistryRequestV2();

        Instant now = Instant.now();
        formatter.format(now);
        request.setEndValidity(formatter.format(now.plus(1, ChronoUnit.DAYS)));
        request.setDescription("description");
        request.setPhoneNumbers(List.of("+390123456789"));
        request.setExternalCodes(List.of("EXT0"));
        request.setEmail("mail@esempio.it");
        request.setAppointmentRequired(true);
        request.setWebsite("https://test.it");
        return request;
    }

    @Test
    void addRegistry_success() {
        CreateRegistryRequestV2 request = buildValidCreateRequest();
        RegistryV2 response = new RegistryV2();
        response.setPartnerId(PARTNER_ID);

        Mockito.when(registrySelfService.addRegistry(eq(PARTNER_ID), anyString(), anyString(), any()))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(CREATE_PATH)
                .header("x-pagopa-pn-cx-type", CxTypeAuthFleet.BO.getValue())
                .header("x-pagopa-pn-cx-id", PARTNER_ID)
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.partnerId").isEqualTo(PARTNER_ID);
    }

    @Test
    void addRegistry_missingRequiredField() {
        CreateRegistryRequestV2 request = buildValidCreateRequest();
        request.setAddress(null);

        webTestClient.post()
                .uri(CREATE_PATH)
                .header("x-pagopa-pn-cx-type", CxTypeAuthFleet.BO.getValue())
                .header("x-pagopa-pn-cx-id", PARTNER_ID)
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void addRegistry_invalidField() {
        CreateRegistryRequestV2 request = buildValidCreateRequest();
        request.setEmail("not-an-email");

        webTestClient.post()
                .uri(CREATE_PATH)
                .header("x-pagopa-pn-cx-type", CxTypeAuthFleet.BO.getValue())
                .header("x-pagopa-pn-cx-id", PARTNER_ID)
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void addRegistry_coordinatesNotFound() {
        CreateRegistryRequestV2 request = buildValidCreateRequest();
        RegistryV2 response = new RegistryV2();
        response.setPartnerId(PARTNER_ID);

        Mockito.when(registrySelfService.addRegistry(eq(PARTNER_ID), anyString(), anyString(), any()))
                .thenReturn(Mono.error(new CoordinatesNotFoundException("Coordinates not found")));

        webTestClient.post()
                .uri(CREATE_PATH)
                .header("x-pagopa-pn-cx-type", CxTypeAuthFleet.BO.getValue())
                .header("x-pagopa-pn-cx-id", PARTNER_ID)
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void updateRegistry_success() {
        UpdateRegistryRequestV2 request = buildValidUpdateRequest();

        RegistryV2 response = new RegistryV2();
        response.setPartnerId(PARTNER_ID);
        response.setLocationId(LOCATION_ID);

        Mockito.when(registrySelfService.updateRegistry(eq(PARTNER_ID), eq(LOCATION_ID), anyString(), any()))
                .thenReturn(Mono.just(response));

        webTestClient.patch()
                .uri(UPDATE_PATH, LOCATION_ID)
                .header("x-pagopa-pn-cx-type", CxTypeAuthFleet.BO.getValue())
                .header("x-pagopa-pn-cx-id", PARTNER_ID)
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
        UpdateRegistryRequestV2 request = buildValidUpdateRequest();
        request.setWebsite("not-a-website");

        webTestClient.patch()
                .uri(UPDATE_PATH, LOCATION_ID)
                .header("x-pagopa-pn-cx-type", CxTypeAuthFleet.BO.getValue())
                .header("x-pagopa-pn-cx-id", PARTNER_ID)
                .header("uid", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

    }


    @Test
    void shouldDeleteRegistryAndReturn204() {
        String partnerId = "partnerTest";
        String locationId = "locationTest";

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        entity.setPartnerId(partnerId);
        entity.setLocationId(locationId);

        when(registrySelfService.deleteRegistry(partnerId, locationId))
                .thenReturn(Mono.just(entity));

        webTestClient.delete()
                     .uri(UPDATE_PATH, locationId)
                     .header("partnerId", partnerId)
                     .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.BO.getValue())
                     .header(PN_PAGOPA_CX_ID, partnerId)
                     .header(PN_PAGOPA_UID, "my-uid")
                     .exchange()
                     .expectStatus().isNoContent();
    }

    @Test
    void shouldReturn204WhenRegistryNotFound() {
        String partnerId = "partnerNotFound";
        String locationId = "locationNotFound";

        when(registrySelfService.deleteRegistry(partnerId, locationId))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                     .uri(UPDATE_PATH, locationId)
                     .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.BO.getValue())
                     .header(PN_PAGOPA_CX_ID, partnerId)
                     .header(PN_PAGOPA_UID, "my-uid")
                     .exchange()
                     .expectStatus().isNoContent(); // 204
    }

    private GetRegistryResponseV2 getRegistryResponseV2() {

        RegistryV2 registry = new RegistryV2();
        registry.setPartnerId(PARTNER_ID);

        List<RegistryV2> listRegistry = new ArrayList<>();
        listRegistry.add(registry);

        GetRegistryResponseV2 res = new GetRegistryResponseV2();
        res.setItems(listRegistry);
        res.setLastKey(LAST_KEY);

        return res;
    }

    @Test
    void retrieveRegistry_success() {

        Mockito.when(registrySelfService.retrieveRegistries(eq(PARTNER_ID), any(), any()))
                .thenReturn(Mono.just(getRegistryResponseV2()));

        String GET_PATH = "/radd-bo/api/v2/registry";
        webTestClient.get()
                .uri(GET_PATH)
                .header("x-pagopa-pn-cx-type", CxTypeAuthFleet.BO.getValue())
                .header("x-pagopa-pn-cx-id", PARTNER_ID)
                .header("uid", "test-uid")
                .exchange()
                .expectStatus()
                .isOk();

    }

}
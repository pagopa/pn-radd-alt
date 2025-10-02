package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Coverage;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateCoverageRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.*;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.services.radd.fsu.v1.CoverageService;
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

@ContextConfiguration(classes = {CoverageController.class, RestExceptionHandler.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {CoverageController.class})
class CoverageControllerTest {

    @MockBean
    private CoverageService coverageService;

    @Autowired
    WebTestClient webTestClient;

    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_UID = "x-pagopa-pn-uid";
    private final String CF_ENTE = "LLLLLLNNLNNLNNNL";
    private final String U_ID = "UID";

    private final String CAP = "00000";
    private final String LOCALITY = "locality";

    private final String CREATE_PATH = "/coverages";

    private CreateCoverageRequest buildValidCreateRequest() {

        CreateCoverageRequest req = new CreateCoverageRequest();
        req.setCap(CAP);
        req.setLocality(LOCALITY);
        req.setProvince("RM");
        req.setCadastralCode("A000");
        return req;

    }

    @Test
    void addCoverage_success() {

        CreateCoverageRequest request = buildValidCreateRequest();
        Coverage response = new Coverage();
        response.setCap(CAP);
        response.setLocality(LOCALITY);

        Mockito.when(coverageService.addCoverage(request))
               .thenReturn(Mono.just(response));

        webTestClient.post()
                     .uri(CREATE_PATH)
                     .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.BO.getValue())
                     .header(PN_PAGOPA_CX_ID, CF_ENTE)
                     .header(PN_PAGOPA_UID, U_ID)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(request)
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody()
                     .jsonPath("$.cap").isEqualTo(CAP);

    }

    @Test
    void addCoverage_missingRequiredField() {

        CreateCoverageRequest request = buildValidCreateRequest();
        request.setCap(null);

        webTestClient.post()
                     .uri(CREATE_PATH)
                     .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.BO.getValue())
                     .header(PN_PAGOPA_CX_ID, CF_ENTE)
                     .header(PN_PAGOPA_UID, U_ID)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(request)
                     .exchange()
                     .expectStatus()
                     .isBadRequest();

    }

    @Test
    void addCoverage_invalidField() {

        CreateCoverageRequest request = buildValidCreateRequest();
        request.setCap("not-a-cap");

        webTestClient.post()
                     .uri(CREATE_PATH)
                     .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.BO.getValue())
                     .header(PN_PAGOPA_CX_ID, CF_ENTE)
                     .header(PN_PAGOPA_UID, U_ID)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(request)
                     .exchange()
                     .expectStatus()
                     .isBadRequest();
    }

}
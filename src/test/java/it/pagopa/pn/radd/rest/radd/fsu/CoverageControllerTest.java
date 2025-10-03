package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Coverage;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.UpdateCoverageRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.*;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.exception.*;
import it.pagopa.pn.radd.services.radd.fsu.v1.CoverageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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

    private final String UPDATE_PATH = "/radd-bo/api/v1/coverages/{cap}/{locality}";

    private UpdateCoverageRequest buildValidUpdateRequest() {

        UpdateCoverageRequest req = new UpdateCoverageRequest();

        req.setProvince("RM");
        req.setCadastralCode("A000");
        req.setStartValidity(LocalDate.now());
        req.setEndValidity(LocalDate.now().plusDays(1L));

        return req;

    }

    @Test
    void updateCoverage_success() {

        UpdateCoverageRequest request = buildValidUpdateRequest();

        Coverage response = new Coverage();
        response.setCap(CAP);
        response.setLocality(LOCALITY);

        when(coverageService.updateCoverage(CAP, LOCALITY, request))
                .thenReturn(Mono.just(response));

        webTestClient.patch()
                     .uri(UPDATE_PATH, CAP, LOCALITY)
                     .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.BO.getValue())
                     .header(PN_PAGOPA_CX_ID, CF_ENTE)
                     .header(PN_PAGOPA_UID, U_ID)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(request)
                     .exchange()
                     .expectStatus()
                     .isOk();

    }

    @Test
    void updateCoverage_NotFound() {

        UpdateCoverageRequest request = buildValidUpdateRequest();

        when(coverageService.updateCoverage(anyString(), anyString(), eq(request)))
                .thenReturn(Mono.error(new RaddGenericException(ExceptionTypeEnum.COVERAGE_NOT_FOUND, HttpStatus.NOT_FOUND)));

        webTestClient.patch()
                     .uri(UPDATE_PATH, CAP, LOCALITY)
                     .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.BO.getValue())
                     .header(PN_PAGOPA_CX_ID, CF_ENTE)
                     .header(PN_PAGOPA_UID, U_ID)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(request)
                     .exchange()
                     .expectStatus()
                     .isNotFound();

    }

    @Test
    void updateCoverage_BadRequest() {

        UpdateCoverageRequest request = buildValidUpdateRequest();
        request.setCadastralCode("");

        when(coverageService.updateCoverage(anyString(), anyString(), eq(request)))
                .thenReturn(Mono.empty());

        webTestClient.patch()
                     .uri(UPDATE_PATH, CAP, LOCALITY)
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
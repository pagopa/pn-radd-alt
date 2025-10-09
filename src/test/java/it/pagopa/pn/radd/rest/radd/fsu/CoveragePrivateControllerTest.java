package it.pagopa.pn.radd.rest.radd.fsu;


import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.SearchMode;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.services.radd.fsu.v1.CoverageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@ContextConfiguration(classes = {CoveragePrivateController.class, RestExceptionHandler.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {CoveragePrivateController.class})
public class CoveragePrivateControllerTest {


    @MockBean
    private CoverageService coverageService;

    @Autowired
    WebTestClient webTestClient;

    private final String SEARCH_MODE="search_mode";
    private final String CAP = "00000";
    private final String LOCALITY = "locality";

    private final String CREATE_PATH_PRIVATE = "/radd-bo/api/v1/coverages/check";



    private CheckCoverageRequest checkCoverageRequest() {
        return new CheckCoverageRequest()
                .cap(CAP)
                .city(LOCALITY);
    }


    @Test
    void checkCoverageTest_success() {

        CheckCoverageResponse  response= new CheckCoverageResponse();
        response.setHasCoverage(true);

        Mockito.when(coverageService.checkCoverage(checkCoverageRequest(), SearchMode.LIGHT))
               .thenReturn(Mono.just(response));

        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder
                             .path(CREATE_PATH_PRIVATE)
                             .queryParam(SEARCH_MODE, "LIGHT")
                             .build())
                     .bodyValue(checkCoverageRequest())
                     .exchange()
                     .expectStatus().isOk();
    }


    @Test
    void checkCoverageTest_badRequest() {

        CheckCoverageResponse  response= new CheckCoverageResponse();
        response.setHasCoverage(true);

        Mockito.when(coverageService.checkCoverage( checkCoverageRequest(), null))
               .thenReturn(Mono.just(response));

        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder
                             .path(CREATE_PATH_PRIVATE)
                             .build())
                     .bodyValue(checkCoverageRequest())
                     .exchange()
                     .expectStatus().isBadRequest();
    }




}

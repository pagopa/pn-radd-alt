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

import java.time.LocalDate;

@ContextConfiguration(classes = {CoveragePrivateController.class, RestExceptionHandler.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {CoveragePrivateController.class})
class CoveragePrivateControllerTest {



    @MockBean
    private CoverageService coverageService;

    @Autowired
    WebTestClient webTestClient;

    private static final String SEARCH_MODE="search_mode";
    private static final String SEARCH_DATE = "search_date";
    private static final String CAP = "00000";
    private static final String LOCALITY = "locality";

    private static final String CREATE_PATH_PRIVATE = "/radd-net-private/api/v1/coverages/check";



    private CheckCoverageRequest checkCoverageRequest() {
        return new CheckCoverageRequest()
                .cap(CAP)
                .city(LOCALITY);
    }


    @Test
    void checkCoverageTest_success() {

        CheckCoverageResponse  response= new CheckCoverageResponse();
        response.setHasCoverage(true);

        Mockito.when(coverageService.checkCoverage(checkCoverageRequest(), SearchMode.LIGHT, null))
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

        Mockito.when(coverageService.checkCoverage( checkCoverageRequest(), null, null))
               .thenReturn(Mono.just(response));

        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder
                             .path(CREATE_PATH_PRIVATE)
                             .build())
                     .bodyValue(checkCoverageRequest())
                     .exchange()
                     .expectStatus().isBadRequest();
    }

    @Test
    void checkCoverageTest_withSearchDate_success() {

        CheckCoverageResponse response = new CheckCoverageResponse();
        response.setHasCoverage(true);

        LocalDate searchDate = LocalDate.of(2025, 6, 15);

        Mockito.when(coverageService.checkCoverage(checkCoverageRequest(), SearchMode.LIGHT, searchDate))
               .thenReturn(Mono.just(response));

        webTestClient.post()
                     .uri(uriBuilder -> uriBuilder
                             .path(CREATE_PATH_PRIVATE)
                             .queryParam(SEARCH_MODE, "LIGHT")
                             .queryParam(SEARCH_DATE, "2025-06-15")
                             .build())
                     .bodyValue(checkCoverageRequest())
                     .exchange()
                     .expectStatus().isOk();
    }

    @Test
    void checkCoverageTest_malformedSearchDate_badRequest() {
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(CREATE_PATH_PRIVATE)
                        .queryParam(SEARCH_MODE, "LIGHT")
                        .queryParam(SEARCH_DATE, "malformed-date")
                        .build())
                .bodyValue(checkCoverageRequest())
                .exchange()
                .expectStatus().isBadRequest();
    }

}

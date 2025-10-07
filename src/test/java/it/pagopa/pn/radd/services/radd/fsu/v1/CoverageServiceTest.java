package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;

class CoverageServiceTest {

    private CoverageDAO coverageDAO;
    private CoverageService coverageService;

    @BeforeEach
    void setUp() {
        coverageDAO = Mockito.mock(CoverageDAO.class);
        coverageService = new CoverageService(coverageDAO);
    }

    @Test
    void testCheckCoverageLightModeWithCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100");
        CoverageEntity coverage = new CoverageEntity();
        coverage.setStartValidity(LocalDate.now().minusDays(1));
        coverage.setEndValidity(LocalDate.now().plusDays(1));

        Mockito.when(coverageDAO.findByCap(eq("00100")))
               .thenReturn(Flux.just(coverage));

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.LIGHT))
                    .expectNextMatches(resp -> resp.getHasCoverage())
                    .verifyComplete();
    }

    @Test
    void testCheckCoverageLightModeWithoutCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100");

        Mockito.when(coverageDAO.findByCap(eq("00100")))
               .thenReturn(Flux.empty());

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.LIGHT))
                    .expectNextMatches(resp -> !resp.getHasCoverage())
                    .verifyComplete();
    }

    @Test
    void testCheckCoverageCompleteModeWithCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100").city("Roma");
        CoverageEntity coverage = new CoverageEntity();
        coverage.setStartValidity(LocalDate.now().minusDays(1));
        coverage.setEndValidity(LocalDate.now().plusDays(1));

        Mockito.when(coverageDAO.find(eq("00100"), eq("Roma")))
               .thenReturn(Mono.just(coverage));

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.COMPLETE))
                    .expectNextMatches(resp -> resp.getHasCoverage())
                    .verifyComplete();
    }

    @Test
    void testCheckCoverageCompleteModeWithoutCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100").city("Roma");

        Mockito.when(coverageDAO.find(eq("00100"), eq("Roma")))
               .thenReturn(Mono.empty());

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.COMPLETE))
                    .expectNextMatches(resp -> !resp.getHasCoverage())
                    .verifyComplete();
    }


    //test per isValidityActive

    @Test
    void testCoverageWithActiveDate() {
        CheckCoverageRequest request = new CheckCoverageRequest();
        request.setCap("00000");
        request.setCity("Roma");

        // Mock del DAO
        CoverageEntity cov = new CoverageEntity();
        cov.setStartValidity(LocalDate.now().minusDays(1));
        cov.setEndValidity(LocalDate.now().plusDays(1));

        Mockito.when(coverageDAO.findByCap("00000"))
               .thenReturn(Flux.just(cov));

        Mono<CheckCoverageResponse> result = coverageService.checkCoverage(request, SearchMode.LIGHT);

        StepVerifier.create(result)
                    .expectNextMatches(resp -> resp.getHasCoverage())
                    .verifyComplete();
    }

    @Test
    void testCoverageWithoutActiveDate() {
        CheckCoverageRequest request = new CheckCoverageRequest();
        request.setCap("00000");
        request.setCity("Roma");

        // Mock del DAO
        CoverageEntity cov = new CoverageEntity();
        cov.setEndValidity(LocalDate.now().minusDays(1));

        Mockito.when(coverageDAO.findByCap("00000"))
               .thenReturn(Flux.just(cov));

        Mono<CheckCoverageResponse> result = coverageService.checkCoverage(request, SearchMode.LIGHT);

        StepVerifier.create(result)
                    .expectNextMatches(resp -> !resp.getHasCoverage())
                    .verifyComplete();
    }


}
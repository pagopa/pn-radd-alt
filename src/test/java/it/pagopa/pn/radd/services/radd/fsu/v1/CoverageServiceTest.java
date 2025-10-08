package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Coverage;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateCoverageRequest;
import it.pagopa.pn.radd.mapper.*;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.SearchMode;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.pn.radd.utils.DateUtils;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {RegistrySelfServiceV2.class})
@CustomLog
class CoverageServiceTest {

    @Mock
    private CoverageDAO coverageDAO;

    @Mock
    private CoverageService coverageService;

    private final String U_ID = UUID.randomUUID().toString();

    private final String CAP = "00000";
    private final String LOCALITY = "locality";

    @BeforeEach
    void setUp() {
        CoverageMapper coverageMapper = new CoverageMapper();
        coverageService = new CoverageService(
                coverageDAO,
                coverageMapper
        );
    }

    private CreateCoverageRequest buildValidCreateRequest() {

        CreateCoverageRequest req = new CreateCoverageRequest();

        req.setCap(CAP);
        req.setLocality(LOCALITY);
        req.setProvince("RM");
        req.setCadastralCode("A000");

        return req;
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
    void addCoverage() {

        CreateCoverageRequest request = buildValidCreateRequest();
        CoverageEntity entity = new CoverageEntity();

        Mockito.lenient().when(coverageDAO.findByCap(CAP)).thenReturn(Flux.empty());
        when(coverageDAO.putItemIfAbsent(any())).thenReturn(Mono.just(entity));

        Mono<Coverage> result = coverageService.addCoverage(U_ID, request);

        StepVerifier.create(result)
                    .assertNext(Assertions::assertNotNull)
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
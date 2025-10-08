package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.pn.radd.utils.DateUtils;
import lombok.CustomLog;
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

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {RegistrySelfServiceV2.class})
@CustomLog
class CoverageServiceTest {

    @Mock
    private CoverageDAO coverageDAO;

    @Mock
    private CoverageService coverageService;


    @BeforeEach
    void setUp() {
        coverageService = new CoverageService(
                coverageDAO
        );
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

    /**
     * Test per util isValidityActive
     */

    //entrambe le date null -> false
    @Test
    void testBothNull() throws Exception {
      assertFalse(DateUtils.isValidityActive(null, null));
    }

    //solo start valorizzata -> controllo se oggi >= start
    @Test
    void testOnlyStartPast() throws Exception {
        LocalDate start = LocalDate.now().minusDays(1);
        assertTrue(DateUtils.isValidityActive(start, null));
    }

    //solo start valorizzata nel futuro -> torno false
    @Test
    void testOnlyStartFuture() throws Exception {
        LocalDate start = LocalDate.now().plusDays(1);
        assertFalse(DateUtils.isValidityActive(start, null));
    }

    //solo end valorizzata nel passato -> torno false
    @Test
    void testOnlyEndPast() throws Exception {
        LocalDate end = LocalDate.now().minusDays(1);
        assertFalse(DateUtils.isValidityActive(null, end));
    }

    //solo end valorizzata nel futuro -> torno true
    @Test
    void testOnlyEndFuture() throws Exception {
        LocalDate end = LocalDate.now().plusDays(1);
        assertTrue(DateUtils.isValidityActive(null, end));
    }

    // entrambe valorizzate e uguali -> controllo se oggi == quella data
    @Test
    void testStartEqualsEndToday() throws Exception {
        LocalDate date = LocalDate.now();
        assertTrue(DateUtils.isValidityActive(date, date));
    }

    // entrambe valorizzate e uguali nel passato -> torno false
    @Test
    void testStartEqualsEndPast() throws Exception {
        LocalDate date = LocalDate.now().minusDays(1);
        assertFalse(DateUtils.isValidityActive(date, date));
    }

    // entrambe valorizzate e uguali nel futuro -> torno false
    @Test
    void testStartEqualsEndFuture() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        assertFalse(DateUtils.isValidityActive(date, date));
    }

    // entrambe valorizzate e diverse -> controllo se oggi è tra start e end (inclusi)
    @Test
    void testRangeIncludesToday() throws Exception {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(1);
        assertTrue(DateUtils.isValidityActive(start, end));
    }

    // entrambe valorizzate e diverse -> oggi non è incluso -> torno false
    @Test
    void testRangeBeforeToday() throws Exception {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now().minusDays(1);
        assertFalse(DateUtils.isValidityActive(start, end));
    }

    // entrambe valorizzate nel futuro -> torno false
    @Test
    void testRangeAfterToday() throws Exception {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(5);
        assertFalse(DateUtils.isValidityActive(start, end));
    }

    //start oggi, end futuro -> torno true
    @Test
    void testRangeStartTodayEndFuture() throws Exception {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(5);
        assertTrue(DateUtils.isValidityActive(start, end));
    }

    //start passato, end oggi -> torno true
    @Test
    void testRangeStartPastEndToday() throws Exception {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now();
        assertTrue(DateUtils.isValidityActive(start, end));
    }

}
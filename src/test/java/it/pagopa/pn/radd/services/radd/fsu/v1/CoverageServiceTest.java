package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.*;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.SearchMode;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;

import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.LocalDate;
import java.util.stream.Stream;
import java.util.UUID;

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

    private static final String U_ID = UUID.randomUUID().toString();

    private static final String CAP = "00000";
    private static final String LOCALITY = "locality";

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

    private UpdateCoverageRequest buildValidUpdateRequest() {

        UpdateCoverageRequest req = new UpdateCoverageRequest();

        req.setProvince("RM");
        req.setCadastralCode("A000");
        req.setStartValidity(LocalDate.now());
        req.setEndValidity(LocalDate.now().plusDays(1L));

        return req;

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
    void updateCoverage() {

        UpdateCoverageRequest request = buildValidUpdateRequest();

        CoverageEntity entity = new CoverageEntity();
        entity.setCap(CAP);
        entity.setLocality(LOCALITY);

        Mockito.lenient().when(coverageDAO.findByCap(CAP)).thenReturn(Flux.empty());
        Mockito.lenient().when(coverageDAO.find(CAP, LOCALITY)).thenReturn(Mono.just(entity));
        when(coverageDAO.updateCoverageEntity(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(coverageService.updateCoverage(U_ID, CAP, LOCALITY, request))
                    .expectNextMatches(coverageEntity -> entity.getCadastralCode().equalsIgnoreCase(request.getCadastralCode())
                                                         && entity.getProvince().equalsIgnoreCase(request.getProvince()))
                    .verifyComplete();

    }

    @Test
    void updateCoverage_NotFound() {

        when(coverageDAO.find(CAP, LOCALITY)).thenReturn(Mono.empty());

        StepVerifier.create(coverageService.updateCoverage(U_ID, CAP, LOCALITY, new UpdateCoverageRequest()))
                    .verifyErrorMessage(ExceptionTypeEnum.COVERAGE_NOT_FOUND.getMessage());

    }

    @Test
    void updateCoverage_BadRequest() {

        CoverageEntity entity = new CoverageEntity();
        entity.setCap(CAP);
        entity.setLocality(LOCALITY);
        entity.setCadastralCode("");

        when(coverageDAO.find(CAP, LOCALITY)).thenReturn(Mono.just(entity));

        StepVerifier.create(coverageService.updateCoverage(U_ID, CAP, LOCALITY, new UpdateCoverageRequest()))
                    .verifyError();

    }

    @Test
    void updateCoverage_InvalidIntervalDates() {

        UpdateCoverageRequest request = buildValidUpdateRequest();
        request.setEndValidity(LocalDate.now().minusDays(5L));

        RaddGenericException ex = Assertions.assertThrows(RaddGenericException.class, () -> coverageService.updateCoverage(U_ID, CAP, LOCALITY, request));
        Assertions.assertEquals(ExceptionTypeEnum.DATE_INTERVAL_ERROR, ex.getExceptionType());

    }

    @Test
    void testCheckCoverageLightModeWithCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100");
        CoverageEntity coverage = new CoverageEntity();
        coverage.setStartValidity(LocalDate.now().minusDays(1));
        coverage.setEndValidity(LocalDate.now().plusDays(1));

        Mockito.when(coverageDAO.findByCap("00100"))
                .thenReturn(Flux.just(coverage));

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.LIGHT, null))
                .expectNextMatches(resp -> resp.getHasCoverage())
                .verifyComplete();
    }

    @Test
    void testCheckCoverageLightModeWithoutCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100");

        Mockito.when(coverageDAO.findByCap("00100"))
                .thenReturn(Flux.empty());

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.LIGHT, null))
                .expectNextMatches(resp -> !resp.getHasCoverage())
                .verifyComplete();
    }

    @Test
    void testCheckCoverageCompleteModeWithCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100").city("Roma");
        CoverageEntity coverage = new CoverageEntity();
        coverage.setStartValidity(LocalDate.now().minusDays(1));
        coverage.setEndValidity(LocalDate.now().plusDays(1));

        Mockito.when(coverageDAO.find("00100", "Roma"))
                .thenReturn(Mono.just(coverage));

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.COMPLETE, null))
                .expectNextMatches(resp -> resp.getHasCoverage())
                .verifyComplete();
    }
    @Test
    void testCheckCoverageCompleteModeWithoutCoverage() {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100").city("Roma");

        Mockito.when(coverageDAO.find("00100", "Roma"))
                .thenReturn(Mono.empty());

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.COMPLETE, null))
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

        Mono<CheckCoverageResponse> result = coverageService.checkCoverage(request, SearchMode.LIGHT, null);

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

        Mono<CheckCoverageResponse> result = coverageService.checkCoverage(request, SearchMode.LIGHT, null);

        StepVerifier.create(result)
                .expectNextMatches(resp -> !resp.getHasCoverage())
                .verifyComplete();
    }

    // Test parametrizzati per searchDate con LIGHT mode
    static Stream<Arguments> provideSearchDateTestCasesLightMode() {
        return Stream.of(
                // Nel range
                Arguments.of(LocalDate.of(2025, 6, 15), true, "searchDate in range"),
                // Prima del range
                Arguments.of(LocalDate.of(2024, 12, 31), false, "searchDate before range"),
                // Dopo il range
                Arguments.of(LocalDate.of(2026, 1, 1), false, "searchDate after range")
        );
    }

    @ParameterizedTest(name = "[{index}] LIGHT mode - {2}")
    @MethodSource("provideSearchDateTestCasesLightMode")
    void testCheckCoverageLightModeWithSearchDate(LocalDate searchDate, boolean expectedCoverage) {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100");
        CoverageEntity coverage = new CoverageEntity();
        coverage.setStartValidity(LocalDate.of(2025, 1, 1));
        coverage.setEndValidity(LocalDate.of(2025, 12, 31));

        Mockito.when(coverageDAO.findByCap("00100"))
                .thenReturn(Flux.just(coverage));

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.LIGHT, searchDate))
                .expectNextMatches(resp -> resp.getHasCoverage() == expectedCoverage)
                .verifyComplete();
    }

    // Test parametrizzati per searchDate con COMPLETE mode
    static Stream<Arguments> provideSearchDateTestCasesCompleteMode() {
        return Stream.of(
                // Nel range
                Arguments.of(LocalDate.of(2025, 6, 15), true, "searchDate in range"),
                // Fuori dal range
                Arguments.of(LocalDate.of(2026, 1, 1), false, "searchDate out of range")
        );
    }

    @ParameterizedTest(name = "[{index}] COMPLETE mode - {2}")
    @MethodSource("provideSearchDateTestCasesCompleteMode")
    void testCheckCoverageCompleteModeWithSearchDate(LocalDate searchDate, boolean expectedCoverage) {
        CheckCoverageRequest request = new CheckCoverageRequest().cap("00100").city("Roma");
        CoverageEntity coverage = new CoverageEntity();
        coverage.setStartValidity(LocalDate.of(2025, 1, 1));
        coverage.setEndValidity(LocalDate.of(2025, 12, 31));

        Mockito.when(coverageDAO.find("00100", "Roma"))
                .thenReturn(Mono.just(coverage));

        StepVerifier.create(coverageService.checkCoverage(request, SearchMode.COMPLETE, searchDate))
                .expectNextMatches(resp -> resp.getHasCoverage() == expectedCoverage)
                .verifyComplete();
    }

}
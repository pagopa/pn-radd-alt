package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.UpdateCoverageRequest;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.*;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
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

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {RegistrySelfServiceV2.class})
@CustomLog
class CoverageServiceTest {

    @Mock
    private CoverageDAO coverageDAO;

    @Mock
    private CoverageService coverageService;

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

    private UpdateCoverageRequest buildValidUpdateRequest() {

        UpdateCoverageRequest req = new UpdateCoverageRequest();

        req.setProvince("RM");
        req.setCadastralCode("A000");
        req.setStartValidity(LocalDate.now());
        req.setEndValidity(LocalDate.now().plusDays(1L));

        return req;

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

        StepVerifier.create(coverageService.updateCoverage(CAP, LOCALITY, request))
                    .expectNextMatches(coverageEntity -> entity.getCadastralCode().equalsIgnoreCase(request.getCadastralCode())
                                                         && entity.getProvince().equalsIgnoreCase(request.getProvince()))
                    .verifyComplete();

    }

    @Test
    void updateCoverage_NotFound() {

        when(coverageDAO.find(CAP, LOCALITY)).thenReturn(Mono.empty());

        StepVerifier.create(coverageService.updateCoverage(CAP, LOCALITY, new UpdateCoverageRequest()))
                    .verifyErrorMessage(ExceptionTypeEnum.COVERAGE_NOT_FOUND.getMessage());

    }

    @Test
    void updateCoverage_BadRequest() {

        CoverageEntity entity = new CoverageEntity();
        entity.setCap(CAP);
        entity.setLocality(LOCALITY);
        entity.setCadastralCode("");

        when(coverageDAO.find(CAP, LOCALITY)).thenReturn(Mono.just(entity));

        StepVerifier.create(coverageService.updateCoverage(CAP, LOCALITY, new UpdateCoverageRequest()))
                    .verifyError();

    }

    @Test
    void updateCoverage_InvalidIntervalDates() {

        UpdateCoverageRequest request = buildValidUpdateRequest();
        request.setEndValidity(LocalDate.now().minusDays(5L));

        RaddGenericException ex = Assertions.assertThrows(RaddGenericException.class, () -> coverageService.updateCoverage(CAP, LOCALITY, request));
        Assertions.assertEquals(ExceptionTypeEnum.COVERAGE_DATE_INTERVAL_ERROR, ex.getExceptionType());

    }

}
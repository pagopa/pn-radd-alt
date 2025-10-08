package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Coverage;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateCoverageRequest;
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

}
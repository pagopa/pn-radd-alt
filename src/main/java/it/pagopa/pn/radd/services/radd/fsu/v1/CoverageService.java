package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.mapper.CoverageMapper;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.radd.utils.CoverageUtils.buildCoverageEntity;

@Service
@RequiredArgsConstructor
@CustomLog
public class CoverageService {

    private final CoverageDAO coverageDAO;
    private final CoverageMapper coverageMapper;

    public Mono<Coverage> addCoverage(String xPagopaPnUid, CreateCoverageRequest request) {
        log.info("Creating coverage entity for cap: {} and locality: {}", request.getCap(), request.getLocality());
        return Mono.defer(() -> coverageDAO.putItemIfAbsent(buildCoverageEntity(xPagopaPnUid, request)))
                   .map(coverageMapper::toDto)
                   .doOnNext(result -> log.debug("Coverage entity with cap: {} and locality: {} created successfully", request.getCap(), request.getLocality()));
    }

}
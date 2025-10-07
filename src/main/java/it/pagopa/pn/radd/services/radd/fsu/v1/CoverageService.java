package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.CoverageMapper;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.radd.utils.CoverageUtils.mapFieldToUpdate;
import static it.pagopa.pn.radd.utils.DateUtils.validateCoverageDateInterval;

@Service
@RequiredArgsConstructor
@CustomLog
public class CoverageService {

    private final CoverageDAO coverageDAO;
    private final CoverageMapper coverageMapper;

    public Mono<Coverage> updateCoverage(String xPagopaPnUid, String cap, String locality, UpdateCoverageRequest request) {
        validateCoverageDateInterval(null, null, request.getStartValidity(), request.getEndValidity());
        log.info("Start updateCoverage for cap [{}] and locality [{}]", cap, locality);
        return coverageDAO.find(cap, locality)
                          .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.COVERAGE_NOT_FOUND, HttpStatus.NOT_FOUND)))
                          .flatMap(coverageEntity -> coverageDAO.updateCoverageEntity(mapFieldToUpdate(xPagopaPnUid, coverageEntity, request)))
                          .map(coverageMapper::toDto)
                          .doOnError(throwable -> log.error("Error during update coverage request", throwable));
    }

}
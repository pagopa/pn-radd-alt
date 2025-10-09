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

import java.time.LocalDate;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.utils.DateUtils;

import static it.pagopa.pn.radd.utils.CoverageUtils.buildCoverageEntity;
import static it.pagopa.pn.radd.utils.CoverageUtils.mapFieldToUpdate;
import static it.pagopa.pn.radd.utils.DateUtils.isValidInterval;

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

    public Mono<Coverage> updateCoverage(String xPagopaPnUid, String cap, String locality, UpdateCoverageRequest request) {
        log.info("Start updateCoverage for cap [{}] and locality [{}]", cap, locality);
        validateInputInterval(request.getStartValidity(), request.getEndValidity());
        return coverageDAO.find(cap, locality)
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.COVERAGE_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(coverageEntity -> coverageDAO.updateCoverageEntity(mapFieldToUpdate(xPagopaPnUid, coverageEntity, request)))
                .map(coverageMapper::toDto)
                .doOnError(throwable -> log.error("Error during update coverage request", throwable));
    }

    private void validateInputInterval(LocalDate startValidity, LocalDate endValidity) {
        if (!isValidInterval(startValidity, endValidity)) {
            throw new RaddGenericException(ExceptionTypeEnum.DATE_INTERVAL_ERROR, HttpStatus.BAD_REQUEST);
        }
    }

    public Mono<CheckCoverageResponse> checkCoverage(CheckCoverageRequest request, SearchMode searchMode) {
        log.info("Checking coverage for cap: {} and locality: {} with search mode: {}",
                request.getCap(), request.getCity(), searchMode);

        return switch (searchMode) {
            case LIGHT -> handleLightSearch(request);
            case COMPLETE -> handleCompleteSearch(request);
        };
    }

    private Mono<CheckCoverageResponse> handleLightSearch(CheckCoverageRequest request) {
        return coverageDAO.findByCap(request.getCap())
                .filter(cov -> DateUtils.isValidityActive(cov.getStartValidity(), cov.getEndValidity()))
                .collectList()
                .map(coverages -> new CheckCoverageResponse().hasCoverage(!coverages.isEmpty()))
                .defaultIfEmpty(new CheckCoverageResponse().hasCoverage(false));
    }

    private Mono<CheckCoverageResponse> handleCompleteSearch(CheckCoverageRequest request) {
        return coverageDAO.find(request.getCap(), request.getCity())
                .map(cov -> new CheckCoverageResponse()
                        .hasCoverage(DateUtils.isValidityActive(cov.getStartValidity(), cov.getEndValidity())))
                .defaultIfEmpty(new CheckCoverageResponse().hasCoverage(false));
    }


}
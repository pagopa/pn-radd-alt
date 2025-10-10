package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.api.CoverageApi;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateCoverageRequest;
import it.pagopa.pn.radd.services.radd.fsu.v1.CoverageService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@CustomLog
public class CoverageController implements CoverageApi {

    private final CoverageService coverageService;

    @Override
    public Mono<ResponseEntity<Coverage>> addCoverage(CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnUid, Mono<CreateCoverageRequest> createCoverageRequest, ServerWebExchange exchange) {
        return createCoverageRequest.flatMap(request -> coverageService.addCoverage(xPagopaPnUid,request))
                                    .map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
    }

    @Override
    public Mono<ResponseEntity<Coverage>> updateCoverage(CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnUid, String cap, String locality, Mono<UpdateCoverageRequest> updateCoverageRequest, ServerWebExchange exchange) {
        return updateCoverageRequest.flatMap(request -> coverageService.updateCoverage(xPagopaPnUid, cap, locality, request))
                                    .map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
    }

}
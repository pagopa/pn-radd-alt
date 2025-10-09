package it.pagopa.pn.radd.rest.radd.fsu;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.api.CoveragePrivateApi;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CheckCoverageResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.SearchMode;
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
public class CoveragePrivateController implements CoveragePrivateApi {

    private final CoverageService coverageService;


    @Override
    public Mono<ResponseEntity<CheckCoverageResponse>> checkCoverage(SearchMode searchMode, Mono<CheckCoverageRequest> checkCoverageRequest, ServerWebExchange exchange) {
        return checkCoverageRequest.flatMap(request -> coverageService.checkCoverage(request, searchMode))
                                   .map(checkCoverageResponse -> ResponseEntity.status(HttpStatus.OK).body(checkCoverageResponse));
        }
    }


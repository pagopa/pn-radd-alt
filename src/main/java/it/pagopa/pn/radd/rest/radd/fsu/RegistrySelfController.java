package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.api.RegistryApi;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.GetRegistryResponseV2;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
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
public class RegistrySelfController implements RegistryApi {

    private final RegistrySelfService registrySelfService;

    @Override
    public Mono<ResponseEntity<RegistryV2>> addRegistry(String xPagopaPnCxId, String uid, String partnerId, Mono<CreateRegistryRequestV2> createRegistryRequestV2, ServerWebExchange exchange) {
        return createRegistryRequestV2.flatMap(request -> registrySelfService.addRegistry(partnerId, request.getLocationId(), uid, request))
                .map(createRegistryResponse -> ResponseEntity.status(HttpStatus.OK).body(createRegistryResponse));
    }

    @Override
    public Mono<ResponseEntity<RegistryV2>> updateRegistry(String xPagopaPnCxId, String uid, String partnerId, String locationId, Mono<UpdateRegistryRequestV2> updateRegistryRequestV2, ServerWebExchange exchange) {
        return updateRegistryRequestV2.flatMap(request -> registrySelfService.updateRegistry(partnerId, locationId, request))
                .map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteRegistry(String xPagopaPnCxId, String uid, String partnerId, String locationId, ServerWebExchange exchange) {
        return registrySelfService.deleteRegistry(partnerId, locationId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<GetRegistryResponseV2>> retrieveRegistry(CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String uid, String partnerId, Integer limit, String lastKey, ServerWebExchange exchange) {
        return registrySelfService.retrieveRegistry(partnerId, limit, lastKey)
                .map(getRegistryResponseV2 -> ResponseEntity.status(HttpStatus.OK).body(getRegistryResponseV2));
    }
}

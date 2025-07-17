package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.api.RegistryApi;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.GetRegistryResponseV2;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class RegistrySelfController implements RegistryApi {

    private final RegistrySelfService registrySelfService;

    @Override
    public Mono<ResponseEntity<GetRegistryResponseV2>> retrieveRegistry(CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, String uid, String partnerId, Integer limit, String lastKey, ServerWebExchange exchange) {
        return registrySelfService.retrieveRegistry(partnerId, limit, lastKey)
                                  .map(getRegistryResponseV2 -> ResponseEntity.status(HttpStatus.OK).body(getRegistryResponseV2));
    }

}
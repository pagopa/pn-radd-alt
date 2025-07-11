package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.api.RegistryApi;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistrySelfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class RegistrySelfController implements RegistryApi {

    private final RegistrySelfService registrySelfService;

    /**
     * DELETE /radd-net/api/v2/registry/{partnerId}/{locationId}
     * API utilizzata per l'eliminazione puntuale di una sede CAF.
     * @param xPagopaPnCxType
     * @param xPagopaPnCxId
     * @param uid
     * @param partnerId
     * @param locationId
     * @param exchange
     * @return OK (status code 204)
     * or Bad Request (status code 400)
     * or Unauthorized (status code 401)
     * or Forbidden (status code 403)
     * or Method not allowed (status code 405)
     * or Internal Server Error (status code 500)
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteRegistry(CxTypeAuthFleet xPagopaPnCxType,
                                                     String xPagopaPnCxId,
                                                     String uid,
                                                     String partnerId,
                                                     String locationId,
                                                     ServerWebExchange exchange) {
        return registrySelfService.deleteRegistry(partnerId, locationId)
                                  .thenReturn(ResponseEntity.noContent().build());
    }

}

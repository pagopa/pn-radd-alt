package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.mapper.RaddRegistryPageMapper;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@CustomLog
public class RegistrySelfService {

    private final RaddRegistryV2DAO raddRegistryDAO;
    private  final RaddRegistryPageMapper raddRegistryPageMapper;

    public Mono<GetRegistryResponseV2> retrieveRegistry(String partnerId, Integer limit, String lastKey) {
        log.info("start retrieveRegistry for partnerId: {}", partnerId);
        return raddRegistryDAO.findPaginatedByPartnerId(partnerId,limit, lastKey)
                              .map(raddRegistryPageMapper::toDto)
                              .doOnError(throwable -> log.error("Error during retrieve registry request for partnerId: {}", partnerId, throwable));
    }

}
package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.middleware.db.entities.RaddStoreLocatorEntity;
import reactor.core.publisher.Mono;

public interface RaddStoreLocatorDAO {
    Mono<RaddStoreLocatorEntity> retrieveLatestStoreLocatorEntity(String csvConfigurationVersion);
    Mono<RaddStoreLocatorEntity> putRaddStoreLocatorEntity(RaddStoreLocatorEntity raddStoreLocatorEntity);
}

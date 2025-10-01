package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CoverageDAO {

    Mono<CoverageEntity> putItemIfAbsent(CoverageEntity newItem);

    Mono<CoverageEntity> updateCoverageEntity(CoverageEntity coverageEntity);

    Mono<CoverageEntity> find(String cap, String locality);

    Flux<CoverageEntity> findByCap(String cap);

}

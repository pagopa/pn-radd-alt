package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.pojo.RaddRegistryPage;
import it.pagopa.pn.radd.pojo.ResultPaginationDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

public interface RaddRegistryV2DAO {

    Mono<RaddRegistryEntityV2> find(String partnerId, String locationId);

    Mono<RaddRegistryEntityV2> updateRegistryEntity(RaddRegistryEntityV2 registryEntity);

    Mono<RaddRegistryEntityV2> putItemIfAbsent(RaddRegistryEntityV2 newItem);

    Flux<RaddRegistryEntityV2> findByPartnerId(String partnerId);

    Mono<RaddRegistryPage> findPaginatedByPartnerId(String partnerId, Integer limit, String lastKey);

    Mono<Page<RaddRegistryEntityV2>> scanRegistries(Integer limit, String lastKey);

    Mono<RaddRegistryEntityV2> delete(String partnerId, String locationId);

    Mono<ResultPaginationDto<RaddRegistryEntityV2, String>> findByFilters(String partnerId, Integer limit, String cap, String city, String pr, String externalCode, String lastEvaluatedKey);

    /**
     * Recupera tutti i registry associati ad un partnerId e ad un requestId.
     *
     * @param partnerId l'ID del partner
     * @param requestId l'ID della richiesta
     * @return un Flux di RaddRegistryEntityV2 che soddisfano i criteri di ricerca
     */
    Flux<RaddRegistryEntityV2> findByPartnerIdAndRequestId(String partnerId, String requestId);

    /**
     * Recupera tutti i registry associati ad un partnerId e che siano stati creati tramite CRUD.
     * I record creati tramite CRUD sono quelli con requestId nullo o che inizia con SELF.
     *
     * @param partnerId l'ID del partner
     * @return un Flux di RaddRegistryEntityV2 creati tramite CRUD
     */
    Flux<RaddRegistryEntityV2> findCrudRegistriesByPartnerId(String partnerId);

}

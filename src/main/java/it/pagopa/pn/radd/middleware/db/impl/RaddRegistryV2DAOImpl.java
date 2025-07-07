package it.pagopa.pn.radd.middleware.db.impl;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.RaddRegistryAlreadyExistsException;
import it.pagopa.pn.radd.exception.TransactionAlreadyExistsException;
import it.pagopa.pn.radd.middleware.db.BaseDao;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import lombok.CustomLog;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Repository
@CustomLog
public class RaddRegistryV2DAOImpl extends BaseDao<RaddRegistryEntityV2> implements RaddRegistryV2DAO {
    private final PnRaddFsuConfig pnRaddFsuConfig;

    public RaddRegistryV2DAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 DynamoDbAsyncClient dynamoDbAsyncClient,
                                 PnRaddFsuConfig raddFsuConfig) {
        super(dynamoDbEnhancedAsyncClient,
                dynamoDbAsyncClient,
                raddFsuConfig.getDao().getRaddRegistryTable(),
                raddFsuConfig,
                RaddRegistryEntityV2.class
        );
        pnRaddFsuConfig = raddFsuConfig;
    }

    @Override
    public Mono<RaddRegistryEntityV2> find(String partnerId, String locationId) {
        Key key = Key.builder().partitionValue(partnerId).sortValue(locationId).build();
        return findFromKey(key);
    }

    @Override
    public Mono<RaddRegistryEntityV2> updateRegistryEntity(RaddRegistryEntityV2 registryEntity) {
        return this.updateItem(registryEntity);
    }

    @Override
    public Mono<RaddRegistryEntityV2> putItemIfAbsent(RaddRegistryEntityV2 newRegistry) {

        Expression condition = Expression.builder()
                .expression("attribute_not_exists(partnerId) AND attribute_not_exists(locationId)")
                .build();

        return this.putItemWithConditions(newRegistry, condition, RaddRegistryEntityV2.class)
                .onErrorMap(TransactionAlreadyExistsException.class, e -> new RaddRegistryAlreadyExistsException());

    }

    @Override
    public Flux<RaddRegistryEntityV2> findByPartnerId(String partnerId) {
        Key key = Key.builder().partitionValue(partnerId).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);

        return getByFilter(conditional, null, null, null, null, null);
    }

    @Override
    public Mono<RaddRegistryEntityV2> delete(String partnerId, String locationId) {
        return deleteItem(Key.builder().partitionValue(partnerId).sortValue(locationId).build());
    }
}

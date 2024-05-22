package it.pagopa.pn.radd.middleware.db.impl;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.middleware.db.BaseDao;
import it.pagopa.pn.radd.middleware.db.RaddStoreLocatorDAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddStoreLocatorEntity;
import lombok.CustomLog;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Repository
@CustomLog
public class RaddStoreLocatorDAOImpl extends BaseDao<RaddStoreLocatorEntity> implements RaddStoreLocatorDAO {
    private final PnRaddFsuConfig pnRaddFsuConfig;
    protected RaddStoreLocatorDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, DynamoDbAsyncClient dynamoDbAsyncClient, PnRaddFsuConfig raddFsuConfig, PnRaddFsuConfig pnRaddFsuConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, raddFsuConfig.getDao().getRaddStoreLocatorTable(), raddFsuConfig, RaddStoreLocatorEntity.class);
        this.pnRaddFsuConfig = pnRaddFsuConfig;
    }

    @Override
    public Mono<RaddStoreLocatorEntity> retrieveLatestStoreLocatorEntity() {
        Key key = Key.builder().partitionValue(pnRaddFsuConfig.getStoreLocator().getCsvType()).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);
        return super.getWithScanIndexForward(conditional, RaddStoreLocatorEntity.CSVTYPE_CREATEDAT_INDEX, 1, false)
                .next();
    }

    @Override
    public Mono<RaddStoreLocatorEntity> putRaddStoreLocatorEntity(RaddStoreLocatorEntity raddStoreLocatorEntity) {
        return putItem(raddStoreLocatorEntity);
    }
}

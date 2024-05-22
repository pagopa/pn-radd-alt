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
    protected RaddStoreLocatorDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, DynamoDbAsyncClient dynamoDbAsyncClient, PnRaddFsuConfig raddFsuConfig) {
        super(dynamoDbEnhancedAsyncClient, dynamoDbAsyncClient, raddFsuConfig.getDao().getRaddStoreLocatorTable(), raddFsuConfig, RaddStoreLocatorEntity.class);
    }

    @Override
    public Mono<RaddStoreLocatorEntity> retrieveLatestStoreLocatorEntity(String csvConfigurationVersion) {
        Key key = Key.builder().partitionValue(csvConfigurationVersion).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);
        return super.getWithScanIndexForward(conditional, RaddStoreLocatorEntity.CSVCONFIGURATIONVERSION_CREATEDAT_INDEX, 1, false)
                .next();
    }

    @Override
    public Mono<RaddStoreLocatorEntity> putRaddStoreLocatorEntity(RaddStoreLocatorEntity raddStoreLocatorEntity) {
        return putItem(raddStoreLocatorEntity);
    }
}

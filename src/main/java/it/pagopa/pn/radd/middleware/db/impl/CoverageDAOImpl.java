package it.pagopa.pn.radd.middleware.db.impl;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.CoverageAlreadyExistsException;
import it.pagopa.pn.radd.exception.TransactionAlreadyExistsException;
import it.pagopa.pn.radd.middleware.db.BaseDao;
import it.pagopa.pn.radd.middleware.db.CoverageDAO;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
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
public class CoverageDAOImpl extends BaseDao<CoverageEntity> implements CoverageDAO {

    public CoverageDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                           DynamoDbAsyncClient dynamoDbAsyncClient,
                           PnRaddFsuConfig raddFsuConfig) {
        super(dynamoDbEnhancedAsyncClient,
              dynamoDbAsyncClient,
              raddFsuConfig.getDao().getRaddCoverageTable(),
              raddFsuConfig,
              CoverageEntity.class
             );
    }

    @Override
    public Mono<CoverageEntity> putItemIfAbsent(CoverageEntity newItem) {
        Expression condition = Expression.builder()
                                         .expression("attribute_not_exists(cap) AND attribute_not_exists(locality)")
                                         .build();

        return this.putItemWithConditions(newItem, condition, CoverageEntity.class)
                   .onErrorMap(TransactionAlreadyExistsException.class, e -> new CoverageAlreadyExistsException());
    }

    @Override
    public Mono<CoverageEntity> updateCoverageEntity(CoverageEntity coverageEntity) {
        return this.updateItem(coverageEntity);
    }

    @Override
    public Mono<CoverageEntity> find(String cap, String locality) {
        Key key = Key.builder().partitionValue(cap).sortValue(locality).build();
        return findFromKey(key);
    }

    @Override
    public Flux<CoverageEntity> findByCap(String cap) {
        Key key = Key.builder().partitionValue(cap).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);

        return getByFilter(conditional, null, null, null, null, null);
    }

}
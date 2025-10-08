package it.pagopa.pn.radd.middleware.db.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.exception.RaddRegistryAlreadyExistsException;
import it.pagopa.pn.radd.exception.TransactionAlreadyExistsException;
import it.pagopa.pn.radd.middleware.db.BaseDao;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.*;
import it.pagopa.pn.radd.pojo.PnLastEvaluatedKey;
import it.pagopa.pn.radd.pojo.RaddRegistryPage;
import it.pagopa.pn.radd.pojo.ResultPaginationDto;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.ERROR_CODE_PN_RADD_ALT_UNSUPPORTED_LAST_EVALUATED_KEY;
import static it.pagopa.pn.radd.utils.Const.REQUEST_ID_PREFIX;
import static it.pagopa.pn.radd.utils.DateUtils.getStartOfDayToday;

@Repository
@CustomLog
public class RaddRegistryV2DAOImpl extends BaseDao<RaddRegistryEntityV2> implements RaddRegistryV2DAO {

    public static final String EQUALS_FORMAT = "#%s.#%s = :%s";

    public RaddRegistryV2DAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                 DynamoDbAsyncClient dynamoDbAsyncClient,
                                 PnRaddFsuConfig raddFsuConfig) {
        super(dynamoDbEnhancedAsyncClient,
              dynamoDbAsyncClient,
              raddFsuConfig.getDao().getRaddRegistryTableV2(),
              raddFsuConfig,
              RaddRegistryEntityV2.class
             );
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
    public Mono<RaddRegistryPage> findPaginatedByPartnerId(String partnerId, Integer limit, String lastKey) {
        Key key = Key.builder().partitionValue(partnerId).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);

        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();

        QueryEnhancedRequest.Builder qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(conditional);

        if (limit != null) {
            qeRequest.limit(limit);
        }

        if (StringUtils.isNotBlank(lastKey)) {
            lastEvaluatedKey = Map.of(RaddRegistryEntityV2.COL_PARTNER_ID, AttributeValue.builder().s(partnerId).build(),RaddRegistryEntityV2.COL_LOCATION_ID, AttributeValue.builder().s(lastKey).build());
        }

        return constructAndExecuteQuery(qeRequest, lastEvaluatedKey, null)
                .map(page -> {
                    RaddRegistryPage raddRegistryPage = new RaddRegistryPage();
                    raddRegistryPage.setItems(page.items());
                    raddRegistryPage.setLastKey(CollectionUtils.isEmpty(page.lastEvaluatedKey()) ? null : page.lastEvaluatedKey().get(RaddRegistryEntityV2.COL_LOCATION_ID).s());
                    return raddRegistryPage;
                });
    }

    @Override
    public Mono<Page<RaddRegistryEntityV2>> scanRegistries(Integer limit, String lastKey) {
        log.info("Start scan RaddRegistryEntity - limit: [{}] and lastKey: [{}].", limit, lastKey);

        Map<String, String> names = new HashMap<>();
        names.put("#endValidity", RaddRegistryEntity.COL_END_VALIDITY);
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":today", AttributeValue.builder().s(String.valueOf(getStartOfDayToday())).build());
        String query = ":today < #endValidity OR attribute_not_exists(#endValidity)";

        PnLastEvaluatedKey lastEvaluatedKey = null;
        if (io.micrometer.core.instrument.util.StringUtils.isNotEmpty(lastKey)) {
            try {
                lastEvaluatedKey = PnLastEvaluatedKey.deserializeInternalLastEvaluatedKey(lastKey);
            } catch (JsonProcessingException e) {
                throw new RaddGenericException(
                        ERROR_CODE_PN_RADD_ALT_UNSUPPORTED_LAST_EVALUATED_KEY,
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            log.debug("First page search");
        }

        return scan(limit, lastEvaluatedKey != null ? lastEvaluatedKey.getInternalLastEvaluatedKey() : null, values, query, names);
    }

    @Override
    public Mono<RaddRegistryEntityV2> delete(String partnerId, String locationId) {
        return deleteItem(Key.builder().partitionValue(partnerId).sortValue(locationId).build());
    }


    @Override
    public Mono<ResultPaginationDto<RaddRegistryEntityV2, String>> findByFilters(String partnerId, Integer limit, String cap, String city, String pr, String externalCode, String lastKey) {
        log.info("Start findAll RaddRegistryEntity - xPagopaPnCxId={} and limit: [{}] and cap: [{}] and city: [{}] and pr: [{}] and externalCode: [{}].", partnerId, limit, cap, city, pr, externalCode);

        PnLastEvaluatedKey lastEvaluatedKey = null;
        if (io.micrometer.core.instrument.util.StringUtils.isNotEmpty(lastKey)) {
            try {
                lastEvaluatedKey = PnLastEvaluatedKey.deserializeInternalLastEvaluatedKey(lastKey);
            } catch (JsonProcessingException e) {
                throw new RaddGenericException(
                        ERROR_CODE_PN_RADD_ALT_UNSUPPORTED_LAST_EVALUATED_KEY,
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            log.debug("First page search");
        }

        //Creazione key per fare la query
        Key key = Key.builder().partitionValue(partnerId).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);

        //Creazione query filtrata e mappa dei valori per i filtri se presenti
        Map<String, AttributeValue> map = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        StringJoiner query = new StringJoiner(" AND ");
        if (io.micrometer.core.instrument.util.StringUtils.isNotEmpty(cap)) {
            map.put(":" + AddressEntity.COL_CAP, AttributeValue.builder().s(cap).build());
            names.put("#" + RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS, RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS);
            names.put("#" + AddressEntity.COL_CAP, AddressEntity.COL_CAP);
            query.add(String.format(EQUALS_FORMAT, RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS, AddressEntity.COL_CAP, AddressEntity.COL_CAP));        }
        if (io.micrometer.core.instrument.util.StringUtils.isNotEmpty(city)) {
            map.put(":" + AddressEntity.COL_CITY, AttributeValue.builder().s(city).build());
            names.put("#" + RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS, RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS);
            names.put("#" + AddressEntity.COL_CITY, AddressEntity.COL_CITY);
            query.add(String.format(EQUALS_FORMAT, RaddRegistryEntity.COL_NORMALIZED_ADDRESS, AddressEntity.COL_CITY, AddressEntity.COL_CITY));
        }
        if (io.micrometer.core.instrument.util.StringUtils.isNotEmpty(pr)) {
            map.put(":" + AddressEntity.COL_PROVINCE, AttributeValue.builder().s(pr).build());
            names.put("#" + AddressEntity.COL_PROVINCE, AddressEntity.COL_PROVINCE);
            names.put("#" + RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS, RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS);
            query.add(String.format(EQUALS_FORMAT, RaddRegistryEntityV2.COL_NORMALIZED_ADDRESS, AddressEntity.COL_PROVINCE, AddressEntity.COL_PROVINCE));
        }
        if (StringUtils.isNotEmpty(externalCode)) {
            map.put(":"+ RaddRegistryEntityV2.COL_EXTERNAL_CODES, AttributeValue.builder().s(externalCode).build());
            names.put("#" + RaddRegistryEntityV2.COL_EXTERNAL_CODES, RaddRegistryEntityV2.COL_EXTERNAL_CODES);
            query.add(String.format("contains(#%s, :%s)", RaddRegistryEntityV2.COL_EXTERNAL_CODES, RaddRegistryEntityV2.COL_EXTERNAL_CODES));
        }

        return getByFilterPaginated(conditional, null, map, names, query.toString(), limit, lastEvaluatedKey != null ? lastEvaluatedKey.getInternalLastEvaluatedKey() : null, SELF_REGISTRY_REQUEST_LAST_EVALUATED_KEY_MAKER);

    }

    private static final Function<RaddRegistryEntityV2, PnLastEvaluatedKey> SELF_REGISTRY_REQUEST_LAST_EVALUATED_KEY_MAKER = keyEntity -> {
        PnLastEvaluatedKey pageLastEvaluatedKey = new PnLastEvaluatedKey();
        pageLastEvaluatedKey.setExternalLastEvaluatedKey(keyEntity.getLocationId());
        pageLastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                RaddRegistryEntityV2.COL_LOCATION_ID, AttributeValue.builder().s(keyEntity.getLocationId()).build(),
                RaddRegistryEntityV2.COL_PARTNER_ID, AttributeValue.builder().s(keyEntity.getPartnerId()).build()
                ));
        return pageLastEvaluatedKey;
    };

    @Override
    public Flux<RaddRegistryEntityV2> findByPartnerIdAndRequestId(String partnerId, String requestId) {
        QueryConditional conditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(partnerId).build());
        String expression = RaddRegistryEntityV2.COL_REQUEST_ID + " = :requestIdVal";
        Map<String, AttributeValue> values = Map.of(":requestIdVal", AttributeValue.builder().s(requestId).build());

        return getByFilter(conditional, null, expression, values, null, null);
    }

    @Override
    public Flux<RaddRegistryEntityV2> findCrudRegistriesByPartnerId(String partnerId) {
        QueryConditional conditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(partnerId).build());
        Map<String, AttributeValue> values = Map.of(":selfPrefix", AttributeValue.builder().s(REQUEST_ID_PREFIX).build());
        String expression = "attribute_not_exists(" + RaddRegistryEntityV2.COL_REQUEST_ID + ") OR begins_with(" + RaddRegistryEntityV2.COL_REQUEST_ID + ", :selfPrefix)";

        return getByFilter(conditional, null, expression, values, null, null);
    }
}
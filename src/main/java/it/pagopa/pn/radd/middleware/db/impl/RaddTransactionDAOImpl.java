package it.pagopa.pn.radd.middleware.db.impl;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.BaseDao;
import it.pagopa.pn.radd.middleware.db.OperationsIunsDAO;
import it.pagopa.pn.radd.middleware.db.RaddTransactionDAO;
import it.pagopa.pn.radd.middleware.db.entities.OperationsIunsEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity;
import it.pagopa.pn.radd.pojo.RaddTransactionStatusEnum;
import it.pagopa.pn.radd.pojo.TransactionData;
import it.pagopa.pn.radd.utils.Const;
import it.pagopa.pn.radd.utils.DateUtils;
import it.pagopa.pn.radd.utils.OperationTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.DATE_VALIDATION_ERROR;
import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.OPERATION_TYPE_UNKNOWN;
import static it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity.COL_OPERATION_TYPE;
import static it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity.COL_TRANSACTION_ID;
import static it.pagopa.pn.radd.utils.Utils.transactionIdBuilder;

@Repository
@Slf4j
public class RaddTransactionDAOImpl extends BaseDao<RaddTransactionEntity> implements RaddTransactionDAO {
    private final OperationsIunsDAO operationsIunsDAO;

    public RaddTransactionDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                                  DynamoDbAsyncClient dynamoDbAsyncClient,
                                  OperationsIunsDAO operationsIunsDAO,
                                  PnRaddFsuConfig pnRaddFsuConfig) {
        super(dynamoDbEnhancedAsyncClient,
                dynamoDbAsyncClient,
                pnRaddFsuConfig.getDao().getRaddTransactionTable(),
                pnRaddFsuConfig,
                RaddTransactionEntity.class);
        this.operationsIunsDAO = operationsIunsDAO;
    }

    @Override
    public Mono<RaddTransactionEntity> createRaddTransaction(RaddTransactionEntity entity, List<OperationsIunsEntity> iunsEntities){
        entity.setCreationTimestamp(Instant.now());
        return putTransactionWithConditions(entity)
                .doOnNext(raddTransaction -> log.debug("[{} - {}] radd transaction created", raddTransaction.getOperationId(), raddTransaction.getIun()))
                .flatMap(raddTransaction -> operationsIunsDAO.putWithBatch(iunsEntities).thenReturn(entity))
                .flatMap(raddTransaction -> updateStatus(raddTransaction, RaddTransactionStatusEnum.STARTED));
    }

    public Mono<RaddTransactionEntity> putTransactionWithConditions(RaddTransactionEntity entity) {
        Expression expression = createExpression(entity);
        return super.putItemWithConditions(entity, expression, RaddTransactionEntity.class);
    }

    private Expression createExpression(RaddTransactionEntity entity) {
        if(OperationTypeEnum.ACT.name().equals(entity.getOperationType())) {
            return buildExpressionForAct(entity);
        }
        else if(OperationTypeEnum.AOR.name().equals(entity.getOperationType())) {
            return buildExpressionForAor(entity);
        }
        else {
            throw new RaddGenericException(OPERATION_TYPE_UNKNOWN);
        }
    }

    /**
     * Condizione utile per la PUT condizionata di una operazione di tipo ACT.
     * Siccome le operazioni di tipo ACT hanno, rispetto a quelle AOR, i campi iun e qrCode valorizzati,
     * viene ri-utilizzato il metodo {@link #buildExpressionForAor(RaddTransactionEntity)} aggiungendo in AND
     * la condizione dei due campi iun e qrCode (a loro volta legati in AND)
     * @param entity
     *
     * @return una espressione per la PUT condizionale per una operazione ACT
     */
    private Expression buildExpressionForAct(RaddTransactionEntity entity) {

        Expression expressionPK = buildExpressionForPK();

        String expressionIunAndQrCode = "iun = :expectedIun AND qrCode = :expectedQrCode";

        Expression expressionForAor = buildCommonConditionsAORAndACT(entity).build();

        Expression expressionOnlyFieldsAct = Expression.builder()
                .expression(expressionIunAndQrCode)
                .putExpressionValue(":expectedIun", AttributeValue.builder().s(entity.getIun()).build())
                .putExpressionValue(":expectedQrCode", AttributeValue.builder().s(entity.getQrCode()).build())
                .build();

        Expression finalExpressionACT = Expression.join(expressionForAor, expressionOnlyFieldsAct, "AND");

        return Expression.join(expressionPK, finalExpressionACT, "OR");

    }

    /**
     * Questa condizione simula il putIfAbsent
     * @return una espressione che simula il putIfAbsent
     */
    private Expression buildExpressionForPK() {
        return Expression.builder()
                .expression("attribute_not_exists(transactionId) AND attribute_not_exists(operationType)")
                .build();
    }

    /**
     * Condizione utile per la PUT condizionata di una operazione di tipo AOR.
     * @param entity
     *
     * @return una espressione per la PUT condizionale per una operazione ACT
     */
    private Expression buildExpressionForAor(RaddTransactionEntity entity) {
        Expression expressionPK = buildExpressionForPK();

        Expression expressionCommonFields = buildCommonConditionsAORAndACT(entity).build();
        return Expression.join(expressionPK, expressionCommonFields, "OR");
    }

    /**
     * Crea una espressione coi campi comuni per le operazioni sia ACT che AOR (non è inclusa la PK)
     * @param entity
     *
     * @return una espressione coi campi comuni per le operazioni sia ACT che AOR (non è inclusa la PK)
     */
    private Expression.Builder buildCommonConditionsAORAndACT(RaddTransactionEntity entity) {
        StringBuilder expressionFieldsNotPK = new StringBuilder().append(
                "recipientId = :expectedRecipientId AND operation_status <> :expectedCompleted AND operation_status <> :expectedAborted");

        if(entity.getFileKey() != null) {
            expressionFieldsNotPK.append(" AND fileKey = :expectedFileKey");
        }

        if(entity.getDelegateId() != null) {
            expressionFieldsNotPK.append(" AND delegateId = :expectedDelegateId");
        }

        if(entity.getOperationStartDate() != null) {
            expressionFieldsNotPK.append(" AND operationStartDate = :expectedOperationStartDate");
        }

        Expression.Builder builder = Expression.builder()
                .expression(expressionFieldsNotPK.toString())
                .putExpressionValue(":expectedRecipientId", AttributeValue.builder().s(entity.getRecipientId()).build())
                .putExpressionValue(":expectedCompleted", AttributeValue.builder().s(Const.COMPLETED).build())
                .putExpressionValue(":expectedAborted", AttributeValue.builder().s(Const.ABORTED).build());

        if(entity.getDelegateId() != null) {
            builder.putExpressionValue(":expectedDelegateId", AttributeValue.builder().s(entity.getDelegateId()).build());
        }

        if(entity.getOperationStartDate() != null) {
            builder.putExpressionValue(":expectedOperationStartDate", AttributeValue.builder().s(entity.getOperationStartDate()).build());
        }
        if(entity.getFileKey() != null) {
            builder.putExpressionValue(":expectedFileKey", AttributeValue.builder().s(entity.getFileKey()).build());
        }

        return builder;
    }


    @Override
    public Mono<RaddTransactionEntity> getTransaction(String cxType, String cxId, String operationId, OperationTypeEnum operationType) {
        Key key = Key.builder()
                    .partitionValue(transactionIdBuilder(CxTypeAuthFleet.valueOf(cxType), cxId, operationId))
                    .sortValue(operationType.name())
                    .build();
        return this.findFromKey(key)
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_EXIST)));
    }

    @Override
    public Mono<RaddTransactionEntity> getTransaction(String transactionId, OperationTypeEnum operationType) {
        Key key = Key.builder()
                .partitionValue(transactionId)
                .sortValue(operationType.name())
                .build();
        return this.findFromKey(key)
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_EXIST)));
    }

    @Override
    public Mono<RaddTransactionEntity> updateStatus(RaddTransactionEntity entity, RaddTransactionStatusEnum status) {
        entity.setStatus(status.name());
        entity.setUpdateTimestamp(Instant.now());
        return this.updateItem(entity)
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_EXIST)))
                .filter(updated -> updated.getStatus().equals(entity.getStatus()))
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_UPDATE_STATUS)));
    }

    @Override
    public Mono<Integer> countFromIunAndStatus(String iun, String recipientId) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();

        String query = RaddTransactionEntity.COL_STATUS + " = :completed" +
                " AND " + RaddTransactionEntity.COL_RECIPIENT_ID + " = :recipientId";
        expressionValues.put(":iun", AttributeValue.builder().s(iun).build());
        expressionValues.put(":completed",  AttributeValue.builder().s(Const.COMPLETED).build());
        expressionValues.put(":recipientId",  AttributeValue.builder().s(recipientId).build());

        log.trace("COUNT DAO TICK {}", new Date().getTime());

        return this.getCounterQuery(expressionValues, query, RaddTransactionEntity.COL_IUN + " = :iun", RaddTransactionEntity.IUN_SECONDARY_INDEX)
                .doOnNext(response -> log.trace("COUNT DAO TOCK {}", new Date().getTime()));
    }


    @Override
    public Flux<RaddTransactionEntity> getTransactionsFromIun(String iun) {
        Key key = Key.builder().partitionValue(iun).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);
        String index = RaddTransactionEntity.IUN_SECONDARY_INDEX;
        return this.getByFilter(conditional, index, null, null, null,null);
    }

    @Override
    public Flux<RaddTransactionEntity> getTransactionsFromFiscalCode(String ensureFiscalCode, Date from, Date to) {
        if (from != null && to != null && from.after(to)){
            return Flux.error(new RaddGenericException(DATE_VALIDATION_ERROR));
        }

        Map<String, AttributeValue> map = new HashMap<>();
        String query = "";

        if (from != null) {
            map.put(":fromDate", AttributeValue.builder().s(DateUtils.formatDate(from)).build());
            query = RaddTransactionEntity.COL_OPERATION_START_DATE + " >= :fromDate";
        }

        if (from != null && to != null) {
            map.put(":toDate", AttributeValue.builder().s(DateUtils.formatDate(to)).build());
            query += " AND " + RaddTransactionEntity.COL_OPERATION_START_DATE + " <= :toDate";
        }

        Key key = Key.builder().partitionValue(ensureFiscalCode).build();
        QueryConditional conditional = QueryConditional.keyEqualTo(key);
        String indexRecipient = RaddTransactionEntity.RECIPIENT_SECONDARY_INDEX;
        String indexDelegate = RaddTransactionEntity.DELEGATE_SECONDARY_INDEX;

        return this.getByFilter(conditional, indexRecipient, query,map, null,null)
                .concatWith(this.getByFilter(conditional, indexDelegate, query,map, null,null))
                .distinct(RaddTransactionEntity::getOperationId);
    }
    @Override
    public Mono<RaddTransactionEntity> updateZipAttachments(RaddTransactionEntity entity, Map<String, String> zipAttachments) {
        entity.setZipAttachments(zipAttachments);
        entity.setUpdateTimestamp(Instant.now());
        return this.updateItem(entity)
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_EXIST)))
                .filter(updated -> updated.getZipAttachments().equals(entity.getZipAttachments()))
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_UPDATE_STATUS)));
    }

    @Override
    public Mono<RaddTransactionEntity> updateDocAttachments(RaddTransactionEntity entity, TransactionData transactionData) {
        entity.setDocAttachments(transactionData.getDocAttachments());
        entity.setUpdateTimestamp(Instant.now());
        return this.updateItem(entity)
                   .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_EXIST)))
                   .filter(updated -> updated.getDocAttachments().equals(entity.getDocAttachments()))
                   .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_UPDATE_STATUS)));
    }

    @Override
    public Mono<Void> addSenderPaId(String transactionId, String operationType, String senderPaIdToAdd) {
        Map<String, AttributeValue> key = Map.of(
                COL_TRANSACTION_ID, AttributeValue.builder().s(transactionId).build(),
                COL_OPERATION_TYPE, AttributeValue.builder().s(operationType).build()
        );

        Map<String, String> expressionAttributes = Map.of(
                "#setAttr", RaddTransactionEntity.COL_SENDER_PA_IDS,
                "#updateTs", RaddTransactionEntity.COL_UPDATE_TIME_STAMP
        );

        Map<String, AttributeValue> expressionValues = Map.of(
                ":valuesToAdd", AttributeValue.builder().ss(senderPaIdToAdd).build(),
                ":currentTs", AttributeValue.builder().s(Instant.now().toString()).build()
        );

        return this.updateItemWithExpression(
                key,
                "ADD #setAttr :valuesToAdd SET #updateTs = :currentTs",
                "attribute_exists(" + COL_TRANSACTION_ID + ") AND attribute_exists(" + COL_OPERATION_TYPE + ")",
                expressionAttributes,
                expressionValues
        )
        .onErrorMap(ConditionalCheckFailedException.class, ex -> new RaddGenericException(ExceptionTypeEnum.TRANSACTION_NOT_EXIST));
    }

}

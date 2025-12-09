package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.entities.OperationsIunsEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity;
import it.pagopa.pn.radd.pojo.RaddTransactionStatusEnum;
import it.pagopa.pn.radd.utils.Const;
import it.pagopa.pn.radd.utils.OperationTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.DATE_VALIDATION_ERROR;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class RaddTransactionDAOImplTest extends BaseTest.WithLocalStack {

    private final Duration d = Duration.ofMillis(3000);
    @Autowired
    @SpyBean
    private RaddTransactionDAO raddTransactionDAO;
    private RaddTransactionEntity baseEntity;
    private final List<OperationsIunsEntity> iunsEntities = new ArrayList<>();

    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    DynamoDbAsyncTable<RaddTransactionEntity> raddTable;

    @BeforeEach
    public void setUp() {
        baseEntity = new RaddTransactionEntity();
        baseEntity.setIun("iun");
        baseEntity.setUid("uid");
        baseEntity.setOperationId("operationId");
        baseEntity.setOperationType(OperationTypeEnum.ACT.toString());
        baseEntity.setStatus(Const.COMPLETED);
        baseEntity.setQrCode("qrcode12345");
        baseEntity.setRecipientId("ABCDEF12G34H567I");
        baseEntity.setFileKey("filekey1");
        baseEntity.setTransactionId("PG#cxId#operationId");
    }

    @Test
    void testCreateRaddTransaction() {

        RaddTransactionEntity response = raddTransactionDAO.createRaddTransaction(baseEntity, iunsEntities).block();
        assertNotNull(response);
        assertEquals(baseEntity.getOperationId(), response.getOperationId());
        assertEquals(baseEntity.getIun(), response.getIun());
        assertEquals(baseEntity.getUid(), response.getUid());
        assertEquals(Const.STARTED, response.getStatus());
        assertEquals(baseEntity.getRecipientType(), response.getRecipientType());
    }

    @Test
    void testUpdateStatus() {

        RaddTransactionEntity response = raddTransactionDAO.updateStatus(baseEntity, RaddTransactionStatusEnum.COMPLETED).block(d);
        assertNotNull(response);
        assertEquals(response.getOperationId(), baseEntity.getOperationId());
        assertEquals(response.getStatus(), baseEntity.getStatus());
    }

    @Test
    void testUpdateZipAttachments() {

        RaddTransactionEntity response = raddTransactionDAO.updateZipAttachments(baseEntity, Map.of("123", "123")).block(d);
        assertNotNull(response);
        assertEquals(response.getOperationId(), baseEntity.getOperationId());
        assertEquals(response.getZipAttachments(), baseEntity.getZipAttachments());
    }

    @Test
    void testWhenGetActTransactionReturnEntity() {
        RaddTransactionEntity response = raddTransactionDAO.getTransaction("PG", "cxId", "operationId", OperationTypeEnum.ACT).block();
        assertNotNull(response);
        assertEquals(response.getOperationType(), baseEntity.getOperationType());
    }

    @Test
    void testWhenGetActTransactionOnThrow() {

        StepVerifier.create(
                raddTransactionDAO.getTransaction("PF", "", "", OperationTypeEnum.ACT)
                ).expectError(RaddGenericException.class).verify();
    }
    

    @Test
    void testGetTransactionsFromIun() {
        String iun = "iun";
        this.raddTransactionDAO.getTransactionsFromIun(iun)
                .map(transaction -> {
                    assertNotNull(transaction);
                    return Mono.empty();
                })
                .blockFirst();
    }

    @Test
    void testGetTransactionsFromFiscalCode() {
        String fiscalCode = "ABCDEF12G34H567I";
        this.raddTransactionDAO.getTransactionsFromFiscalCode(fiscalCode, new Date(), new Date())
                .map(transaction -> {
                    assertNotNull(transaction);
                    return Mono.empty();
                })
                .blockFirst();

        this.raddTransactionDAO.getTransactionsFromFiscalCode(fiscalCode, null, null)
                .map(transaction -> {
                    assertNotNull(transaction);
                    return Mono.empty();
                })
                .blockFirst();

        this.raddTransactionDAO.getTransactionsFromFiscalCode(fiscalCode, null, new Date())
                .map(transaction -> {
                    assertNotNull(transaction);
                    return Mono.empty();
                })
                .blockFirst();

        this.raddTransactionDAO.getTransactionsFromFiscalCode(fiscalCode, new Date(), null)
                .map(transaction -> {
                    assertNotNull(transaction);
                    return Mono.empty();
                })
                .blockFirst();
    }

    @Test
    void testGetTransactionsFromFiscalCodeError() throws ParseException {
        String fiscalCode = "ABCDEF12G34H567I";
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN);
        String from = "03/11/2022";
        String to = "02/11/2022";
        Date dateFrom = formatter.parse(from);
        Date dateTo = formatter.parse(to);
        Flux<RaddTransactionEntity> response = raddTransactionDAO.getTransactionsFromFiscalCode(fiscalCode, dateFrom, dateTo);
        response.onErrorResume(exception -> {
            if (exception instanceof RaddGenericException){
                assertEquals(DATE_VALIDATION_ERROR.getMessage(), ((RaddGenericException) exception).getExceptionType().getMessage());
                return Mono.empty();
            }
            fail("Badly type exception");
            return Mono.empty();
        }).blockFirst();
    }


    @Test
    void testCreateTransactionWithOperationIunsNotEmpty() {
        String uid = "uid";
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setUid(uid);
        raddTransactionEntity.setOperationType(OperationTypeEnum.AOR.name());
        List<OperationsIunsEntity> entityIuns = new ArrayList<>();
        OperationsIunsEntity operationsIunsEntity = new OperationsIunsEntity();
        operationsIunsEntity.setTransactionId(baseEntity.getOperationId());
        operationsIunsEntity.setIun(baseEntity.getIun());
        entityIuns.add(operationsIunsEntity);
        Mono<RaddTransactionEntity> entityMono = raddTransactionDAO.createRaddTransaction(raddTransactionEntity, entityIuns);
        entityMono.map(entity -> {
            assertEquals(uid, entity.getUid());
            return Mono.empty();
        });
    }

    @Test
    void testCreateTransactionWithOperationIunsEmptyAndNull() {
        List<OperationsIunsEntity> entityIuns = null;
        Mono<RaddTransactionEntity> entityMono = raddTransactionDAO.createRaddTransaction(baseEntity, entityIuns);
        entityMono.map(entity -> {
            assertEquals(baseEntity.getUid(), entity.getUid());
            return Mono.empty();
        });

        entityIuns = new ArrayList<>();
        entityMono = raddTransactionDAO.createRaddTransaction(baseEntity, entityIuns);
        entityMono.map(entity -> {
            assertEquals(baseEntity.getUid(), entity.getUid());
            return Mono.empty();
        });
    }

    @Test
    void testAddSenderPaIdSuccessfully() {
        // Arrange: Create and save a transaction entity
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setTransactionId("PG#testCxId#addSenderPaIdOp1");
        entity.setOperationType(OperationTypeEnum.AOR.name());
        entity.setOperationId("addSenderPaIdOp1");
        entity.setRecipientId("TESTFC12D34E567F");
        entity.setUid("test-uid-1");
        entity.setStatus(Const.STARTED);

        // Create the entity first
        RaddTransactionEntity createdEntity = raddTransactionDAO.createRaddTransaction(entity, new ArrayList<>()).block();
        assertNotNull(createdEntity);

        // Act: Add a senderPaId to the entity
        String senderPaIdToAdd = "PA-001";
        Mono<Void> addResult = raddTransactionDAO.addSenderPaId(createdEntity.getTransactionId(), createdEntity.getOperationType(), senderPaIdToAdd);

        // Assert: Verify the operation completes successfully
        StepVerifier.create(addResult)
                .verifyComplete();

        // Verify the entity was updated by retrieving it
        RaddTransactionEntity retrievedEntity = raddTransactionDAO.getTransaction(
                createdEntity.getTransactionId(),
                OperationTypeEnum.valueOf(createdEntity.getOperationType())
        ).block();

        assertNotNull(retrievedEntity);
        assertNotNull(retrievedEntity.getSenderPaIds());
        assertNotNull(retrievedEntity.getUpdateTimestamp());
        assertTrue(retrievedEntity.getSenderPaIds().contains(senderPaIdToAdd));
        assertEquals(1, retrievedEntity.getSenderPaIds().size());
    }

    @Test
    void testAddSenderPaIdToEmptySet() {
        // Arrange: Create entity without any senderPaIds
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setTransactionId("PG#testCxId#addSenderPaIdOp2");
        entity.setOperationType(OperationTypeEnum.AOR.name());
        entity.setOperationId("addSenderPaIdOp2");
        entity.setRecipientId("TESTFC12D34E567F");
        entity.setUid("test-uid-2");
        entity.setStatus(Const.STARTED);
        entity.setSenderPaIds(null); // Explicitly set to null

        RaddTransactionEntity createdEntity = raddTransactionDAO.createRaddTransaction(entity, new ArrayList<>()).block();
        assertNotNull(createdEntity);

        // Act: Add first senderPaId to an empty/null set
        String senderPaIdToAdd = "PA-FIRST";
        Mono<Void> addResult = raddTransactionDAO.addSenderPaId(createdEntity.getTransactionId(), createdEntity.getOperationType(), senderPaIdToAdd);

        // Assert: Verify successful completion
        StepVerifier.create(addResult)
                .verifyComplete();

        // Verify the set was created and contains the value
        RaddTransactionEntity retrievedEntity = raddTransactionDAO.getTransaction(
                createdEntity.getTransactionId(),
                OperationTypeEnum.valueOf(createdEntity.getOperationType())
        ).block();

        assertNotNull(retrievedEntity);
        assertNotNull(retrievedEntity.getSenderPaIds());
        assertNotNull(retrievedEntity.getUpdateTimestamp());
        assertTrue(retrievedEntity.getSenderPaIds().contains(senderPaIdToAdd));
        assertEquals(1, retrievedEntity.getSenderPaIds().size());
    }

    @Test
    void testAddMultipleSenderPaIdsSequentially() {
        // Arrange: Create base entity
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setTransactionId("PG#testCxId#addSenderPaIdOp3");
        entity.setOperationType(OperationTypeEnum.AOR.name());
        entity.setOperationId("addSenderPaIdOp3");
        entity.setRecipientId("TESTFC12D34E567F");
        entity.setUid("test-uid-3");
        entity.setStatus(Const.STARTED);

        RaddTransactionEntity createdEntity = raddTransactionDAO.createRaddTransaction(entity, new ArrayList<>()).block();
        assertNotNull(createdEntity);

        // Act: Add multiple senderPaIds sequentially
        String senderPaId1 = "PA-MULTI-001";
        String senderPaId2 = "PA-MULTI-002";
        String senderPaId3 = "PA-MULTI-003";

        raddTransactionDAO.addSenderPaId(createdEntity.getTransactionId(), createdEntity.getOperationType(), senderPaId1).block();
        raddTransactionDAO.addSenderPaId(createdEntity.getTransactionId(), createdEntity.getOperationType(), senderPaId2).block();
        raddTransactionDAO.addSenderPaId(createdEntity.getTransactionId(), createdEntity.getOperationType(), senderPaId3).block();

        // Assert: Verify all three were added
        RaddTransactionEntity retrievedEntity = raddTransactionDAO.getTransaction(
                createdEntity.getTransactionId(),
                OperationTypeEnum.valueOf(createdEntity.getOperationType())
        ).block();

        assertNotNull(retrievedEntity);
        assertNotNull(retrievedEntity.getSenderPaIds());
        assertNotNull(retrievedEntity.getUpdateTimestamp());
        assertEquals(3, retrievedEntity.getSenderPaIds().size());
        assertTrue(retrievedEntity.getSenderPaIds().contains(senderPaId1));
        assertTrue(retrievedEntity.getSenderPaIds().contains(senderPaId2));
        assertTrue(retrievedEntity.getSenderPaIds().contains(senderPaId3));
    }

    @Test
    void testAddDuplicateSenderPaId() {
        // Arrange: Create entity and add initial senderPaId
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setTransactionId("PG#testCxId#addSenderPaIdOp4");
        entity.setOperationType(OperationTypeEnum.AOR.name());
        entity.setOperationId("addSenderPaIdOp4");
        entity.setRecipientId("TESTFC12D34E567F");
        entity.setUid("test-uid-4");
        entity.setStatus(Const.STARTED);

        RaddTransactionEntity createdEntity = raddTransactionDAO.createRaddTransaction(entity, new ArrayList<>()).block();
        assertNotNull(createdEntity);

        String duplicateSenderPaId = "PA-DUPLICATE";

        // Add the senderPaId first time
        raddTransactionDAO.addSenderPaId(createdEntity.getTransactionId(), createdEntity.getOperationType(), duplicateSenderPaId).block();

        // Verify it was added
        RaddTransactionEntity afterFirstAdd = raddTransactionDAO.getTransaction(
                createdEntity.getTransactionId(),
                OperationTypeEnum.valueOf(createdEntity.getOperationType())
        ).block();

        assertNotNull(afterFirstAdd);
        assertNotNull(afterFirstAdd.getSenderPaIds());
        assertEquals(1, afterFirstAdd.getSenderPaIds().size());
        assertTrue(afterFirstAdd.getSenderPaIds().contains(duplicateSenderPaId));

        // Act: Add the same senderPaId again (duplicate)
        Mono<Void> addDuplicateResult = raddTransactionDAO.addSenderPaId(createdEntity.getTransactionId(), createdEntity.getOperationType(), duplicateSenderPaId);

        // Assert: Verify operation completes successfully
        StepVerifier.create(addDuplicateResult)
                .verifyComplete();

        // Verify the set still contains only one instance (sets don't allow duplicates)
        RaddTransactionEntity afterSecondAdd = raddTransactionDAO.getTransaction(
                createdEntity.getTransactionId(),
                OperationTypeEnum.valueOf(createdEntity.getOperationType())
        ).block();

        assertNotNull(afterSecondAdd);
        assertNotNull(afterSecondAdd.getSenderPaIds());
        assertNotNull(afterSecondAdd.getUpdateTimestamp());
        assertEquals(1, afterSecondAdd.getSenderPaIds().size());
        assertTrue(afterSecondAdd.getSenderPaIds().contains(duplicateSenderPaId));
    }

}
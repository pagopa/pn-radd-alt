package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryRequestEntity;
import it.pagopa.pn.radd.pojo.ImportStatus;
import it.pagopa.pn.radd.pojo.RegistryRequestStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RaddRegistryRequestDAOImplTest extends BaseTest.WithLocalStack{
    @Autowired
    @SpyBean
    private RaddRegistryRequestDAO registryRequestDAO;
    private RaddRegistryRequestEntity baseEntity;
    @Autowired
    private BaseDao<RaddRegistryRequestEntity> baseDao;
    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    DynamoDbAsyncTable<RaddRegistryRequestEntity> dynamoDbAsyncTable;

    @BeforeEach
    public void setUp() {
        baseEntity = new RaddRegistryRequestEntity();
        baseEntity.setPk("testPk");
        baseEntity.setRequestId("testRequestId");
        baseEntity.setCorrelationId("testCorrelationId");
        baseEntity.setCreatedAt(Instant.now());
        baseEntity.setUpdatedAt(Instant.now());
        baseEntity.setOriginalRequest("testOriginalRequest");
        baseEntity.setZipCode("testZipCode");
        baseEntity.setStatus(ImportStatus.TO_PROCESS.name());
        baseEntity.setError("testError");
        baseEntity.setCxId("testCxId");
        baseEntity.setRegistryId("testRegistryId");
    }
    @Test
    void testPutRaddRegistryImportEntity(){
        RaddRegistryRequestEntity response = baseDao.putItem(baseEntity).block();
        assertNotNull(response);
        Assertions.assertEquals(baseEntity.getPk(), response.getPk());
        Assertions.assertEquals(baseEntity.getCxId(), response.getCxId());
        Assertions.assertEquals(baseEntity.getRegistryId(), response.getRegistryId());
        Assertions.assertEquals(baseEntity.getRequestId(), response.getRequestId());
        Assertions.assertEquals(baseEntity.getCorrelationId(), response.getCorrelationId());
        Assertions.assertEquals(baseEntity.getCreatedAt(), response.getCreatedAt());
        Assertions.assertEquals(baseEntity.getUpdatedAt(), response.getUpdatedAt());
        Assertions.assertEquals(baseEntity.getOriginalRequest(), response.getOriginalRequest());
        Assertions.assertEquals(baseEntity.getZipCode(), response.getZipCode());
        Assertions.assertEquals(baseEntity.getStatus(), response.getStatus());
        Assertions.assertEquals(baseEntity.getError(), response.getError());
    }
    @Test
    void testFindByCorrelationIdWithStatus(){
        RaddRegistryRequestEntity response= registryRequestDAO.findByCorrelationIdWithStatus(baseEntity.getCorrelationId(), ImportStatus.valueOf(baseEntity.getStatus())).blockFirst();
        Assertions.assertNotNull(response);
        Assertions.assertEquals(baseEntity.getCorrelationId(), response.getCorrelationId());
        Assertions.assertEquals(baseEntity.getStatus(), response.getStatus());
    }
    @Test
    void testUpdateRegistryRequestStatus(){
        RaddRegistryRequestEntity object = new RaddRegistryRequestEntity();
        object.setPk("Pk");
        object.setCorrelationId("correlationId");
        object.setRequestId("RequestId");
        object.setStatus(ImportStatus.REJECTED.name());
        object.setUpdatedAt(Instant.now());
        RaddRegistryRequestEntity response= registryRequestDAO.findByCorrelationIdWithStatus(baseEntity.getCorrelationId(), ImportStatus.valueOf(baseEntity.getStatus())).blockFirst();
        StepVerifier.create(registryRequestDAO.updateRegistryRequestStatus(object, RegistryRequestStatus.valueOf(object.getStatus())))
                .expectNextMatches(updatedEntity -> updatedEntity.getRequestId().equals(object.getRequestId()))
                .verifyComplete();
    }
    @Test
    void testUpdateStatusAndError(){
        RaddRegistryRequestEntity object = new RaddRegistryRequestEntity();
        object.setPk("Pk");
        object.setCorrelationId("correlationId");
        object.setRequestId("RequestId");
        object.setStatus(ImportStatus.REJECTED.name());
        object.setUpdatedAt(Instant.now());
        object.setError("error");
        RaddRegistryRequestEntity response= registryRequestDAO.findByCorrelationIdWithStatus(baseEntity.getCorrelationId(), ImportStatus.valueOf(baseEntity.getStatus())).blockFirst();
        StepVerifier.create(registryRequestDAO.updateStatusAndError(object, ImportStatus.valueOf(object.getStatus()), object.getError()))
                .expectNextMatches(updatedEntity -> updatedEntity.getRequestId().equals(object.getRequestId()))
                .verifyComplete();
    }
    @Test
    void testUpdateRecordsInPending(){
        RaddRegistryRequestEntity object = new RaddRegistryRequestEntity();
        object.setPk("Pk");
        object.setCorrelationId("correlationId");
        object.setRequestId("RequestId");
        object.setStatus(ImportStatus.PENDING.name());
        List<RaddRegistryRequestEntity> addresses = new ArrayList<>();
        addresses.add(object);
        Mono<Void> result = registryRequestDAO.updateRecordsInPending(addresses);
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }
    @Test
    void testGetAllFromCorrelationId(){
        RaddRegistryRequestEntity response= registryRequestDAO.findByCorrelationIdWithStatus(baseEntity.getCorrelationId(), ImportStatus.valueOf(baseEntity.getStatus())).blockFirst();
        StepVerifier.create(registryRequestDAO.getAllFromCorrelationId(baseEntity.getCorrelationId(), baseEntity.getStatus()))
                .expectNextMatches(updatedEntity -> updatedEntity.getRequestId().equals(baseEntity.getRequestId()))
                .verifyComplete();
    }
}

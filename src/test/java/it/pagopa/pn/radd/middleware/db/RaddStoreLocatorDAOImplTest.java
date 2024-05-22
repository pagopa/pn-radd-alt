package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.middleware.db.entities.RaddStoreLocatorEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static it.pagopa.pn.radd.pojo.StoreLocatorStatusEnum.TO_UPLOAD;
import static it.pagopa.pn.radd.pojo.StoreLocatorStatusEnum.UPLOADED;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RaddStoreLocatorDAOImplTest extends BaseTest.WithLocalStack{
    @Autowired
    @SpyBean
    private RaddStoreLocatorDAO raddStoreLocatorDAO;
    private RaddStoreLocatorEntity baseEntity;
    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    DynamoDbAsyncTable<RaddStoreLocatorEntity> dynamoDbAsyncTable;

    @BeforeEach
    public void setUp() {
        baseEntity = new RaddStoreLocatorEntity();
        baseEntity.setPk("testPk");
        baseEntity.setCsvConfigurationVersion("1");
        baseEntity.setCreatedAt(Instant.now());
        baseEntity.setDigest("digest");
        baseEntity.setVersionId("1");
        baseEntity.setStatus(TO_UPLOAD.name());


    }

    @Test
    void testPutRaddStoreLocatorEntity(){
        RaddStoreLocatorEntity response = raddStoreLocatorDAO.putRaddStoreLocatorEntity(baseEntity).block();
        assertNotNull(response);
        Assertions.assertEquals(baseEntity.getPk(), response.getPk());
        Assertions.assertEquals(baseEntity.getDigest(), response.getDigest());
        Assertions.assertEquals(baseEntity.getCsvConfigurationVersion(), response.getCsvConfigurationVersion());
        Assertions.assertEquals(baseEntity.getVersionId(), response.getVersionId());
        Assertions.assertEquals(baseEntity.getCreatedAt(), response.getCreatedAt());
        Assertions.assertEquals(baseEntity.getStatus(), response.getStatus());
    }

    @Test
    void testPutRaddStoreLocatorEntity2(){
        StepVerifier.create(raddStoreLocatorDAO.putRaddStoreLocatorEntity(baseEntity))
                .expectNextMatches(foundedEntity -> foundedEntity.getPk().equals(baseEntity.getPk()))
                .verifyComplete();
    }

    @Test
    void testRetrieveLatestStoreLocatorEntity(){
        raddStoreLocatorDAO.putRaddStoreLocatorEntity(baseEntity).block();
        StepVerifier.create(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity("1"))
                .expectNextMatches(foundedEntity -> foundedEntity.getPk().equals(baseEntity.getPk()))
                .verifyComplete();
    }

    @Test
    void testRetrieveLatestStoreLocatorEntityWith3Element(){
        raddStoreLocatorDAO.putRaddStoreLocatorEntity(baseEntity).block();
        RaddStoreLocatorEntity raddStoreLocatorEntity2 = constructRaddStoreLocatorEntity(5);
        raddStoreLocatorDAO.putRaddStoreLocatorEntity(raddStoreLocatorEntity2).block();
        RaddStoreLocatorEntity raddStoreLocatorEntity3 = constructRaddStoreLocatorEntity(8);
        raddStoreLocatorDAO.putRaddStoreLocatorEntity(raddStoreLocatorEntity3).block();

        StepVerifier.create(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity("1"))
                .expectNextMatches(foundedEntity -> foundedEntity.getPk().equals(baseEntity.getPk()))
                .verifyComplete();
    }

    private RaddStoreLocatorEntity constructRaddStoreLocatorEntity(Integer days) {
        RaddStoreLocatorEntity baseEntity = new RaddStoreLocatorEntity();
        baseEntity.setPk("testPk"+days);
        baseEntity.setCsvConfigurationVersion("1");
        baseEntity.setCreatedAt(Instant.now().minus(days, ChronoUnit.DAYS));
        baseEntity.setDigest("digest");
        baseEntity.setVersionId("1");
        baseEntity.setStatus(UPLOADED.name());
        return baseEntity;
    }

}

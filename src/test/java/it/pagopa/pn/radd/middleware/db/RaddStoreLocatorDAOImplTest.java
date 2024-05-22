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

import static it.pagopa.pn.radd.pojo.StoreLocatorStatusEnum.TO_UPLOAD;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RaddStoreLocatorDAOImplTest extends BaseTest.WithLocalStack{
    @Autowired
    @SpyBean
    private RaddStoreLocatorDAO raddStoreLocatorDAO;
    private RaddStoreLocatorEntity baseEntity;
    @Autowired
    private BaseDao<RaddStoreLocatorEntity> baseDao;
    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    DynamoDbAsyncTable<RaddStoreLocatorEntity> dynamoDbAsyncTable;

    @BeforeEach
    public void setUp() {
        baseEntity = new RaddStoreLocatorEntity();
        baseEntity.setPk("testPk");
        baseEntity.setCsvConfigurationVersion("TABLE");
        baseEntity.setCreatedAt(Instant.now());
        baseEntity.setDigest("digest");
        baseEntity.setVersionId("versionId");
        baseEntity.setStatus(TO_UPLOAD.name());


    }

    @Test
    void testPutRaddStoreLocatorEntity(){
        RaddStoreLocatorEntity response = baseDao.putItem(baseEntity).block();
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
    void testGetStoreLocator(){
        StepVerifier.create(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity("1"))
                .expectNextMatches(foundedEntity -> foundedEntity.getPk().equals(baseEntity.getPk()))
                .verifyComplete();
    }

}

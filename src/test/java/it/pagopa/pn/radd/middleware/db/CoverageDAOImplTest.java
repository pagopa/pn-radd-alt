package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@CustomLog
class CoverageDAOImplTest extends BaseTest.WithLocalStack {

    @Autowired
    @SpyBean
    private CoverageDAO raddCoverageDAO;

    @Autowired
    @SpyBean
    private RestExceptionHandler exceptionHandler;

    @Autowired
    private BaseDao<CoverageEntity> baseDao;

    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;

    @Mock
    DynamoDbAsyncTable<CoverageEntity> raddCoverageImportEntityDynamoDbAsyncTable;


    private CoverageEntity buildEntity() {
        CoverageEntity entity = new CoverageEntity();
        entity.setCap("00043");
        entity.setLocality("Ciampino");
        entity.setProvince("RM");
        entity.setCadastralCode("M272");
        entity.setStartValidity(LocalDate.now());
        entity.setEndValidity(LocalDate.now().plusDays(1L));
        return entity;
    }

    @Test
    void shouldInsertAndFindEntity() {
        CoverageEntity entity = buildEntity();

        StepVerifier.create(raddCoverageDAO.putItemIfAbsent(entity))
                    .assertNext(inserted -> assertThat(inserted).isEqualTo(entity))
                    .verifyComplete();

        StepVerifier.create(raddCoverageDAO.find(entity.getCap(), entity.getLocality()))
                    .assertNext(found -> assertThat(found).isEqualTo(entity))
                    .verifyComplete();
    }

    @Test
    void shouldUpdateEntityDescription() {
        CoverageEntity entity = buildEntity();
        entity.setLocality("Initial");

        StepVerifier.create(raddCoverageDAO.putItemIfAbsent(entity))
                    .assertNext(inserted -> assertThat(inserted.getLocality()).isEqualTo("Initial"))
                    .verifyComplete();

        entity.setLocality("Updated");

        StepVerifier.create(raddCoverageDAO.updateCoverageEntity(entity))
                    .assertNext(updated -> assertThat(updated.getLocality()).isEqualTo("Updated"))
                    .verifyComplete();
    }

    @Test
    void shouldFindCoverageByCap() {
        CoverageEntity entity1 = buildEntity();
        CoverageEntity entity2 = buildEntity();
        entity1.setCap("random");
        entity2.setCap(entity1.getCap());
        entity2.setLocality("random");

        StepVerifier.create(
                raddCoverageDAO.putItemIfAbsent(entity1)
                               .then(raddCoverageDAO.putItemIfAbsent(entity2))
                               .then()
                           ).verifyComplete();

        StepVerifier.create(raddCoverageDAO.findByCap(entity1.getCap()).collectList())
                    .assertNext(list -> {
                        assertThat(list).hasSizeGreaterThanOrEqualTo(2);
                        assertThat(list).extracting(CoverageEntity::getCap)
                                        .allMatch(cap -> cap.equals(entity1.getCap()));
                    })
                    .verifyComplete();
    }
}
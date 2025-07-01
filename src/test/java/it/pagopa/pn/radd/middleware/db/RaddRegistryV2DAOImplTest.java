package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.middleware.db.entities.NormalizedAddressEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@CustomLog
public class RaddRegistryV2DAOImplTest extends BaseTest.WithLocalStack {
    @Autowired
    @SpyBean
    private RaddRegistryV2DAO raddRegistryDAO;

    @Autowired
    @SpyBean
    private RestExceptionHandler exceptionHandler;
    private RaddRegistryEntityV2 baseEntity;

    @Autowired
    private BaseDao<RaddRegistryEntityV2> baseDao;
    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    DynamoDbAsyncTable<RaddRegistryEntityV2> raddRegistryImportEntityDynamoDbAsyncTable;

    private RaddRegistryEntityV2 buildEntity() {
        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        entity.setPartnerId("partner-" + UUID.randomUUID());
        entity.setLocationId("loc-" + UUID.randomUUID());
        entity.setExternalCodes(List.of("EXT1", "EXT2"));
        entity.setDescription("Test description");
        entity.setEmail("test@example.com");
        entity.setPhoneNumbers(List.of("123456789"));
        entity.setCapacity("100");
        entity.setOpeningTime("08:00-18:00");
        entity.setStartValidity(Instant.now());
        entity.setEndValidity(Instant.now().plusSeconds(3600));
        entity.setAppointmentRequired(Boolean.TRUE);
        entity.setWebsite("https://test.it");
        entity.setPartnerType("TYPE1");

        NormalizedAddressEntity normalizedAddress = new NormalizedAddressEntity();
        normalizedAddress.setAddressRow("123 Test St");
        normalizedAddress.setCity("Test City");
        normalizedAddress.setPr("TP");
        normalizedAddress.setCountry("Italy");
        normalizedAddress.setLongitude("12.345678");
        normalizedAddress.setLatitude("34.567890");
        normalizedAddress.setBiasPoint(90);
        entity.setNormalizedAddress(normalizedAddress);

        entity.setCreationTimestamp(Instant.now());
        entity.setUpdateTimestamp(Instant.now());
        entity.setUpdateTimestamp(Instant.now());
        return entity;
    }

    @Test
    void testPutAndFind() {
        RaddRegistryEntityV2 entity = buildEntity();

        Mono<RaddRegistryEntityV2> insert = raddRegistryDAO.putItemIfAbsent(entity);

        StepVerifier.create(insert)
                .expectNextMatches(found -> found.equals(entity))
                .verifyComplete();

        Mono<RaddRegistryEntityV2> find = raddRegistryDAO.find(entity.getPartnerId(), entity.getLocationId());

        StepVerifier.create(find)
                .expectNextMatches(found -> found.equals(entity))
                .verifyComplete();
    }

    @Test
    void testUpdate() {
        RaddRegistryEntityV2 entity = buildEntity();
        entity.setDescription("Initial");

        Mono<RaddRegistryEntityV2> insert = raddRegistryDAO.putItemIfAbsent(entity);
        StepVerifier.create(insert)
                .expectNextMatches(found -> "Initial".equals(found.getDescription()))
                .verifyComplete();

        entity.setDescription("Updated");
        Mono<RaddRegistryEntityV2> update = raddRegistryDAO.updateRegistryEntity(entity);
        StepVerifier.create(update)
                .expectNextMatches(updated -> "Updated".equals(updated.getDescription()))
                .verifyComplete();
    }

    @Test
    void testDelete() {
        RaddRegistryEntityV2 entity = buildEntity();

        Mono<RaddRegistryEntityV2> test = raddRegistryDAO.putItemIfAbsent(entity)
                .then(raddRegistryDAO.delete(entity.getPartnerId(), entity.getLocationId()))
                .then(raddRegistryDAO.find(entity.getPartnerId(), entity.getLocationId()));

        StepVerifier.create(test)
                .expectNextMatches(found -> found.equals(entity))
                .verifyComplete();
    }

    @Test
    void testFindByPartnerId() {
        RaddRegistryEntityV2 entity1 = buildEntity();
        RaddRegistryEntityV2 entity2 = buildEntity();
        entity2.setPartnerId(entity1.getPartnerId());

        Mono<Void> setup = raddRegistryDAO.putItemIfAbsent(entity1)
                .then(raddRegistryDAO.putItemIfAbsent(entity2))
                .then();

        StepVerifier.create(setup)
                .verifyComplete();

        StepVerifier.create(raddRegistryDAO.findByPartnerId(entity1.getPartnerId()).collectList())
                .expectNextMatches(list -> list.size() >= 2)
                .verifyComplete();
    }

}

package it.pagopa.pn.radd.middleware.db;

import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.config.RestExceptionHandler;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.entities.BiasPointEntity;
import it.pagopa.pn.radd.middleware.db.entities.NormalizedAddressEntityV2;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.pojo.ResultPaginationDto;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@CustomLog
class RaddRegistryV2DAOImplTest extends BaseTest.WithLocalStack {
    @Autowired
    @MockitoSpyBean
    private RaddRegistryV2DAO raddRegistryDAO;

    @Autowired
    @MockitoSpyBean
    private RestExceptionHandler exceptionHandler;

    @Mock
    ResultPaginationDto resultPaginationDto;
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
        entity.setOpeningTime("08:00-18:00");
        entity.setStartValidity(Instant.now());
        entity.setEndValidity(Instant.now().plusSeconds(3600));
        entity.setAppointmentRequired(Boolean.TRUE);
        entity.setWebsite("https://test.it");
        entity.setPartnerType("TYPE1");

        NormalizedAddressEntityV2 normalizedAddress = getNormalizedAddressEntityV2();

        entity.setNormalizedAddress(normalizedAddress);

        entity.setCreationTimestamp(Instant.now());
        entity.setUpdateTimestamp(Instant.now());
        entity.setUpdateTimestamp(Instant.now());
        return entity;
    }

    private static @NotNull NormalizedAddressEntityV2 getNormalizedAddressEntityV2() {
        NormalizedAddressEntityV2 normalizedAddress = new NormalizedAddressEntityV2();
        normalizedAddress.setAddressRow("123 Test St");
        normalizedAddress.setCity("Test City");
        normalizedAddress.setProvince("TP");
        normalizedAddress.setCountry("Italy");
        normalizedAddress.setLongitude("12.345678");
        normalizedAddress.setLatitude("34.567890");

        BiasPointEntity biasPoint = new BiasPointEntity();
        biasPoint.setOverall(BigDecimal.ONE);
        biasPoint.setAddressNumber(BigDecimal.ONE);
        biasPoint.setLocality(BigDecimal.ONE);
        biasPoint.setSubRegion(BigDecimal.ONE);
        biasPoint.setPostalCode(BigDecimal.ONE);
        biasPoint.setCountry(BigDecimal.ONE);
        normalizedAddress.setBiasPoint(biasPoint);
        return normalizedAddress;
    }

    @Test
    void shouldInsertAndFindEntity() {
        // given
        RaddRegistryEntityV2 entity = buildEntity();

        // when + then
        StepVerifier.create(raddRegistryDAO.putItemIfAbsent(entity))
                .assertNext(inserted -> assertThat(inserted).isEqualTo(entity))
                .verifyComplete();

        StepVerifier.create(raddRegistryDAO.find(entity.getPartnerId(), entity.getLocationId()))
                .assertNext(found -> assertThat(found).isEqualTo(entity))
                .verifyComplete();
    }

    @Test
    void shouldUpdateEntityDescription() {
        // given
        RaddRegistryEntityV2 entity = buildEntity();
        entity.setDescription("Initial");

        StepVerifier.create(raddRegistryDAO.putItemIfAbsent(entity))
                .assertNext(inserted -> assertThat(inserted.getDescription()).isEqualTo("Initial"))
                .verifyComplete();

        // when
        entity.setDescription("Updated");

        // then
        StepVerifier.create(raddRegistryDAO.updateRegistryEntity(entity))
                .assertNext(updated -> assertThat(updated.getDescription()).isEqualTo("Updated"))
                .verifyComplete();
    }

    @Test
    void shouldDeleteEntityAndNotFindIt() {
        // given
        RaddRegistryEntityV2 entity = buildEntity();

        StepVerifier.create(raddRegistryDAO.putItemIfAbsent(entity))
                .assertNext(inserted -> assertThat(inserted).isEqualTo(entity))
                .verifyComplete();

        // when
        StepVerifier.create(raddRegistryDAO.delete(entity.getPartnerId(), entity.getLocationId()))
                .assertNext(deleted -> assertThat(deleted).isEqualTo(entity))
                .verifyComplete();

        // then
        StepVerifier.create(raddRegistryDAO.find(entity.getPartnerId(), entity.getLocationId()))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void shouldFindAllEntitiesByPartnerId() {
        // given
        RaddRegistryEntityV2 entity1 = buildEntity();
        RaddRegistryEntityV2 entity2 = buildEntity();
        entity2.setPartnerId(entity1.getPartnerId()); // stesso partnerId

        StepVerifier.create(
                raddRegistryDAO.putItemIfAbsent(entity1)
                        .then(raddRegistryDAO.putItemIfAbsent(entity2))
                        .then()
        ).verifyComplete();

        // when + then
        StepVerifier.create(raddRegistryDAO.findByPartnerId(entity1.getPartnerId()).collectList())
                .assertNext(list -> {
                    assertThat(list).hasSizeGreaterThanOrEqualTo(2);
                    assertThat(list).extracting(RaddRegistryEntityV2::getPartnerId)
                            .allMatch(pid -> pid.equals(entity1.getPartnerId()));
                })
                .verifyComplete();
    }

    @Test
    void shouldFindEntitiesPaginatedByPartnerId() {
        // given
        RaddRegistryEntityV2 entity1 = buildEntity();
        RaddRegistryEntityV2 entity2 = buildEntity();
        entity2.setPartnerId(entity1.getPartnerId());

        StepVerifier.create(
                raddRegistryDAO.putItemIfAbsent(entity1)
                        .then(raddRegistryDAO.putItemIfAbsent(entity2))
                        .then()
        ).verifyComplete();

        var readPaginatedItems = raddRegistryDAO.findPaginatedByPartnerId(entity1.getPartnerId(), 1, null)
                .flatMap(page -> raddRegistryDAO.findPaginatedByPartnerId(entity1.getPartnerId(), 1, page.getLastKey()))
                .flatMap(page -> raddRegistryDAO.findPaginatedByPartnerId(entity1.getPartnerId(), 1, page.getLastKey()));

        // when + then
        StepVerifier.create(readPaginatedItems)
                .assertNext(page -> Assertions.assertNull(page.getLastKey()))
                .verifyComplete();
    }

    @Test
    void shouldFindAllEntitiesPaginatedByPartnerId() {
        // given
        RaddRegistryEntityV2 entity1 = buildEntity();
        RaddRegistryEntityV2 entity2 = buildEntity();
        entity2.setPartnerId(entity1.getPartnerId());

        StepVerifier.create(
                raddRegistryDAO.putItemIfAbsent(entity1)
                        .then(raddRegistryDAO.putItemIfAbsent(entity2))
                        .then()
        ).verifyComplete();

        var readPaginatedItems = raddRegistryDAO.findPaginatedByPartnerId(entity1.getPartnerId(), null, null);

        // when + then
        StepVerifier.create(readPaginatedItems)
                .assertNext(page -> {
                    log.info("Page: {}", page);
                    Assertions.assertNotNull(page.getItems());
                    Assertions.assertEquals(2, page.getItems().size());
                    Assertions.assertNull(page.getLastKey());
                })
                .verifyComplete();
    }

    @Test
    void scanRegistriesLastKeyNull() {
        RaddRegistryEntityV2 entity1 =  buildEntity();
        RaddRegistryEntityV2 entity2 = buildEntity();

        raddRegistryDAO.putItemIfAbsent(entity1).then(raddRegistryDAO.putItemIfAbsent(entity2)).block();

        StepVerifier.create(raddRegistryDAO.scanRegistries(1, null))
                .expectNextMatches(raddRegistryEntityPage -> raddRegistryEntityPage.items().size() == 1 &&
                        raddRegistryEntityPage.lastEvaluatedKey() != null)
                .verifyComplete();
    }

    @Test
    void scanRegistriesInvalidLastKeyNotNull() {
        RaddRegistryEntityV2 entity1 =  buildEntity();
        RaddRegistryEntityV2 entity2 = buildEntity();

        raddRegistryDAO.putItemIfAbsent(entity1).then(raddRegistryDAO.putItemIfAbsent(entity2)).block();

        Assertions.assertThrows(RaddGenericException.class, () -> raddRegistryDAO.scanRegistries(1, "test"));
    }


    @Test
    void shouldFindByFiltersSuccessfully() {
        // given
        RaddRegistryEntityV2 entity = buildEntity();
        entity.getNormalizedAddress().setCap("00100");
        entity.getNormalizedAddress().setCity("Roma");
        entity.getNormalizedAddress().setProvince("RM");
        entity.setExternalCodes(List.of("EXTCODE"));

        StepVerifier.create(raddRegistryDAO.putItemIfAbsent(entity))
                    .assertNext(inserted -> assertThat(inserted).isEqualTo(entity))
                    .verifyComplete();

        // filtro per cap
        StepVerifier.create(raddRegistryDAO.findByFilters(entity.getPartnerId(), 10, "00100", null, null, null, null))
                    .assertNext(result -> {
                        assertThat(result.getResultsPage()).extracting(e -> e.getNormalizedAddress().getCap()).contains("00100");
                    })
                    .verifyComplete();

        // filtro per city
        StepVerifier.create(raddRegistryDAO.findByFilters(entity.getPartnerId(), 10, null, "Roma", null, null, null))
                    .assertNext(result -> {
                        assertThat(result.getResultsPage()).extracting(e -> e.getNormalizedAddress().getCity()).contains("Roma");
                    })
                    .verifyComplete();

        // filtro per province
        StepVerifier.create(raddRegistryDAO.findByFilters(entity.getPartnerId(), 10, null, null, "RM", null, null))
                    .assertNext(result -> {
                        assertThat(result.getResultsPage()).extracting(e -> e.getNormalizedAddress().getProvince()).contains("RM");
                    })
                    .verifyComplete();

        // filtro per externalCode
        StepVerifier.create(raddRegistryDAO.findByFilters(entity.getPartnerId(), 10, null, null, null, "EXTCODE", null))
                    .assertNext(result -> {
                        assertThat(result.getResultsPage()).extracting(RaddRegistryEntityV2::getExternalCodes).anySatisfy(list -> assertThat(list).contains("EXTCODE"));
                    })
                    .verifyComplete();
    }


    @Test
    void shouldThrowExceptionOnInvalidLastKey() {
        RaddRegistryEntityV2 entity = buildEntity();
        entity.getNormalizedAddress().setCap("00100");
        Assertions.assertThrows(RaddGenericException.class, () ->
                                        raddRegistryDAO.findByFilters(entity.getPartnerId(), 10, "00100", null, null, null, "invalid-last-key").block()
                               );
    }
    @Test
    void shouldFindEntitiesByPartnerIdAndRequestId() {
        // given
        String partnerId = "partner-" + UUID.randomUUID();
        String requestId = "req-123";

        RaddRegistryEntityV2 entity1 = buildEntity();
        entity1.setPartnerId(partnerId);
        entity1.setRequestId(requestId);

        RaddRegistryEntityV2 entity2 = buildEntity();
        entity2.setPartnerId(partnerId);
        entity2.setRequestId(requestId);

        RaddRegistryEntityV2 entity3 = buildEntity();
        entity3.setPartnerId(partnerId);
        entity3.setRequestId("different-req");

        RaddRegistryEntityV2 entity4 = buildEntity();
        entity4.setPartnerId(partnerId);
        entity4.setRequestId(null);

        StepVerifier.create(
                raddRegistryDAO.putItemIfAbsent(entity1)
                        .then(raddRegistryDAO.putItemIfAbsent(entity2))
                        .then(raddRegistryDAO.putItemIfAbsent(entity3))
                        .then(raddRegistryDAO.putItemIfAbsent(entity4))
                        .then()
        ).verifyComplete();

        // when + then
        StepVerifier.create(raddRegistryDAO.findByPartnerIdAndRequestId(partnerId, requestId).collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list).extracting(RaddRegistryEntityV2::getRequestId)
                            .allMatch(rid -> rid.equals(requestId));
                    assertThat(list).extracting(RaddRegistryEntityV2::getPartnerId)
                            .allMatch(pid -> pid.equals(partnerId));
                })
                .verifyComplete();
    }

    @Test
    void shouldFindCrudRegistriesByPartnerId() {
        // given
        String partnerId = "partner-" + UUID.randomUUID();

        RaddRegistryEntityV2 entityWithoutRequestId = buildEntity();
        entityWithoutRequestId.setPartnerId(partnerId);
        entityWithoutRequestId.setRequestId(null);

        RaddRegistryEntityV2 entityWithSelfPrefix = buildEntity();
        entityWithSelfPrefix.setPartnerId(partnerId);
        entityWithSelfPrefix.setRequestId("SELF-123");

        RaddRegistryEntityV2 entityWithImportRequestId = buildEntity();
        entityWithImportRequestId.setPartnerId(partnerId);
        entityWithImportRequestId.setRequestId("import-req-456");

        StepVerifier.create(
                raddRegistryDAO.putItemIfAbsent(entityWithoutRequestId)
                        .then(raddRegistryDAO.putItemIfAbsent(entityWithSelfPrefix))
                        .then(raddRegistryDAO.putItemIfAbsent(entityWithImportRequestId))
                        .then()
        ).verifyComplete();

        // when + then
        StepVerifier.create(raddRegistryDAO.findCrudRegistriesByPartnerId(partnerId).collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list).extracting(RaddRegistryEntityV2::getPartnerId)
                            .allMatch(pid -> pid.equals(partnerId));
                    assertThat(list).extracting(RaddRegistryEntityV2::getRequestId)
                            .allMatch(rid -> rid == null || rid.startsWith("SELF"));
                })
                .verifyComplete();
    }

    @Test
    void shouldNotFindEntitiesWithDifferentRequestId() {
        // given
        String partnerId = "partner-" + UUID.randomUUID();
        String requestId = "req-123";

        RaddRegistryEntityV2 entity = buildEntity();
        entity.setPartnerId(partnerId);
        entity.setRequestId("different-req");

        StepVerifier.create(raddRegistryDAO.putItemIfAbsent(entity))
                .assertNext(inserted -> assertThat(inserted).isEqualTo(entity))
                .verifyComplete();

        // when + then
        StepVerifier.create(raddRegistryDAO.findByPartnerIdAndRequestId(partnerId, requestId).collectList())
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

    @Test
    void shouldNotFindCrudRegistriesWhenAllHaveImportRequestId() {
        // given
        String partnerId = "partner-" + UUID.randomUUID();

        RaddRegistryEntityV2 entity1 = buildEntity();
        entity1.setPartnerId(partnerId);
        entity1.setRequestId("import-req-1");

        RaddRegistryEntityV2 entity2 = buildEntity();
        entity2.setPartnerId(partnerId);
        entity2.setRequestId("import-req-2");

        StepVerifier.create(
                raddRegistryDAO.putItemIfAbsent(entity1)
                        .then(raddRegistryDAO.putItemIfAbsent(entity2))
                        .then()
        ).verifyComplete();

        // when + then
        StepVerifier.create(raddRegistryDAO.findCrudRegistriesByPartnerId(partnerId).collectList())
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

}

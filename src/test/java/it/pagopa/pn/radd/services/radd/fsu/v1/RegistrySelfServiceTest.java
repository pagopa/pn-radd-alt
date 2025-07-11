package it.pagopa.pn.radd.services.radd.fsu.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateRegistryRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateRegistryResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.GeoLocation;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.UpdateRegistryRequest;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.RaddRegistryRequestEntityMapper;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.middleware.db.RaddRegistryDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryRequestDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryRequestEntity;
import it.pagopa.pn.radd.middleware.queue.producer.CorrelationIdEventsProducer;
import it.pagopa.pn.radd.pojo.PnLastEvaluatedKey;
import it.pagopa.pn.radd.pojo.ResultPaginationDto;
import it.pagopa.pn.radd.utils.ObjectMapperUtil;
import it.pagopa.pn.radd.middleware.queue.producer.RaddAltCapCheckerProducer;
import org.junit.jupiter.api.Assertions;
import it.pagopa.pn.radd.utils.RaddRegistryUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {RegistrySelfService.class})
class RegistrySelfServiceTest {

    @Mock
    private RaddRegistryDAO raddRegistryDAO;

    @Mock
    RaddRegistryV2DAO raddRegistryV2DAO;

    @Mock
    private RaddRegistryRequestDAO registryRequestDAO;
    @Mock
    private CorrelationIdEventsProducer correlationIdEventsProducer;
    private final RaddRegistryRequestEntityMapper raddRegistryRequestEntityMapper = new RaddRegistryRequestEntityMapper(new ObjectMapperUtil(new ObjectMapper()));
    @Mock
    private SecretService secretService;
    private RaddRegistryUtils raddRegistryUtils;
    private RegistrySelfService registrySelfService;
    @Mock
    private RaddAltCapCheckerProducer raddAltCapCheckerProducer;
    @Mock
    private PnRaddFsuConfig pnRaddFsuConfig;

    @BeforeEach
    void setUp() {
        registrySelfService = new RegistrySelfService(raddRegistryDAO, raddRegistryV2DAO,registryRequestDAO,raddRegistryRequestEntityMapper,correlationIdEventsProducer,raddAltCapCheckerProducer,
                new RaddRegistryUtils(new ObjectMapperUtil(new ObjectMapper()),pnRaddFsuConfig,secretService),pnRaddFsuConfig);
    }

    @Test
    void updateRegistryNotFound() {
        UpdateRegistryRequest updateRegistryRequest = new UpdateRegistryRequest();
        when(raddRegistryDAO.find("registryId", "cxId")).thenReturn(Mono.empty());
        StepVerifier.create(registrySelfService.updateRegistry("registryId", "cxId", updateRegistryRequest))
                .verifyErrorMessage("Punto di ritiro SEND non trovato");
    }

    @Test
    void updateRegistry() {
        String newDescription = "new description";
        String newPhoneNumber = "0600011231";
        UpdateRegistryRequest updateRegistryRequest = new UpdateRegistryRequest();
        updateRegistryRequest.setDescription(newDescription);
        updateRegistryRequest.setPhoneNumber(newPhoneNumber);
        RaddRegistryEntity entity = new RaddRegistryEntity();
        entity.setRegistryId("registryId");
        when(raddRegistryDAO.find("registryId", "cxId")).thenReturn(Mono.just(entity));
        when(raddRegistryDAO.updateRegistryEntity(entity)).thenReturn(Mono.just(entity));
        StepVerifier.create(registrySelfService.updateRegistry("registryId", "cxId", updateRegistryRequest))
                .expectNextMatches(raddRegistryEntity -> entity.getDescription().equalsIgnoreCase(newDescription)
                        && entity.getPhoneNumber().equalsIgnoreCase(newPhoneNumber))
                .verifyComplete();
    }

    @Test
    public void shouldAddRegistrySuccessfully() {
        CreateRegistryRequest request = new CreateRegistryRequest();
        request.setPhoneNumber("+39 0123456");
        request.setCapacity("100");
        request.setOpeningTime("mon=10:00-13:00_14:00-20:00#tue=10:00-20:00#thu=10:00-20:00#");

        GeoLocation geoLocation = new GeoLocation();
        geoLocation.setLatitude("42.12345");
        geoLocation.setLongitude("51.12345");
        request.setGeoLocation(geoLocation);

        RaddRegistryRequestEntity entity = new RaddRegistryRequestEntity();
        entity.setRequestId("testRequestId");
        when(registryRequestDAO.createEntity(any())).thenReturn(Mono.just(entity));
        doNothing().when(correlationIdEventsProducer).sendCorrelationIdEvent(any());

        Mono<CreateRegistryResponse> result = registrySelfService.addRegistry("cxId", request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("testRequestId", response.getRequestId());
                })
                .verifyComplete();
    }

    @Test
    public void shouldAddRegistrySuccessfullyWithWrongOpeningTime() {
        CreateRegistryRequest request = new CreateRegistryRequest();
        request.setPhoneNumber("+39 0123456");
        request.setCapacity("100");
        request.setOpeningTime("mon=10:00-13:00_14:00-20:00;tue=10:00-20:00;thu=10:00-20:00;");

        GeoLocation geoLocation = new GeoLocation();
        geoLocation.setLatitude("42.12345");
        geoLocation.setLongitude("51.12345");
        request.setGeoLocation(geoLocation);

        RaddRegistryRequestEntity entity = new RaddRegistryRequestEntity();
        entity.setRequestId("testRequestId");
        Assertions.assertThrows(RaddGenericException.class, () -> registrySelfService.addRegistry("cxId", request));

    }



    @Test
    public void shouldAddRegistryFailsForInvalidIntervalDates() {
        CreateRegistryRequest request = new CreateRegistryRequest();
        request.setStartValidity("2024-03-01");
        request.setEndValidity("2023-10-21");

        Assertions.assertThrows(RaddGenericException.class, () -> registrySelfService.addRegistry("cxId", request));
    }

    @Test
    public void shouldAddRegistryFailsForInvalidDateFormat() {
        CreateRegistryRequest request = new CreateRegistryRequest();
        request.setStartValidity("10/02/2022");

        Assertions.assertThrows(RaddGenericException.class, () -> registrySelfService.addRegistry("cxId", request));
    }

    @Test
    public void shouldAddRegistryFailsForGeolocationFormat() {
        CreateRegistryRequest request = new CreateRegistryRequest();
        GeoLocation geoLocation = new GeoLocation();
        geoLocation.setLatitude("10.0");
        geoLocation.setLongitude("10,0");
        request.setGeoLocation(geoLocation);

        Assertions.assertThrows(RaddGenericException.class, () -> registrySelfService.addRegistry("cxId", request));
    }

    @Test
    public void shouldAddRegistryFailsForOpeningTimeFormat() {
        CreateRegistryRequest request = new CreateRegistryRequest();
        request.setOpeningTime("10:00");

        Assertions.assertThrows(RaddGenericException.class, () -> registrySelfService.addRegistry("cxId", request));
    }

    @Test
    public void shouldAddRegistryFailsForCapacityFormat() {
        CreateRegistryRequest request = new CreateRegistryRequest();
        request.setCapacity("10a");

        Assertions.assertThrows(RaddGenericException.class, () -> registrySelfService.addRegistry("cxId", request));
    }

    @Test
    void registryListing() {
        ResultPaginationDto<RaddRegistryEntity, String> paginator = new ResultPaginationDto<RaddRegistryEntity, String>().toBuilder().build();
        paginator.setResultsPage(List.of());
        PnLastEvaluatedKey lastEvaluatedKeyToSerialize = new PnLastEvaluatedKey();
        lastEvaluatedKeyToSerialize.setExternalLastEvaluatedKey( "SenderId##creationMonth" );
        lastEvaluatedKeyToSerialize.setInternalLastEvaluatedKey(
                Map.of( "KEY", AttributeValue.builder()
                        .s( "VALUE" )
                        .build() )  );
        String serializedLEK = lastEvaluatedKeyToSerialize.serializeInternalLastEvaluatedKey();
        when(raddRegistryDAO.findByFilters(eq("cxId"), eq(1),eq("cap"), eq("city"), eq("pr"), eq("externalCode"), any())).thenReturn(Mono.just(paginator));
        StepVerifier.create(registrySelfService.registryListing("cxId", 1, serializedLEK,"cap", "city", "pr", "externalCode"))
                .expectNextMatches(registriesResponse -> Boolean.FALSE.equals(registriesResponse.getMoreResult()))
                .verifyComplete();
    }

    @Test
    void shouldDeleteRegistrySuccessfully() {
        // Given
        String partnerId = "partnerTest";
        String locationId = "locationTest";

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        entity.setPartnerId(partnerId);
        entity.setLocationId(locationId);


        when(raddRegistryV2DAO.delete(partnerId, locationId)).thenReturn(Mono.just(entity));

        Mono<RaddRegistryEntityV2> result = registrySelfService.deleteRegistry(partnerId, locationId);

        StepVerifier.create(result)
                    .expectNextMatches(deleted -> deleted.getPartnerId().equals(partnerId) && deleted.getLocationId().equals(locationId))
                    .verifyComplete();
    }


    @Test
    void shouldCompleteDeleteWhenRegistryNotFound() {
        // Given
        String partnerId = "partnerTest";
        String locationId = "locationTest";

        when(raddRegistryV2DAO.delete(partnerId, locationId)).thenReturn(Mono.empty());

        Mono<RaddRegistryEntityV2> result = registrySelfService.deleteRegistry(partnerId, locationId);

        StepVerifier.create(result).verifyComplete();
    }
}

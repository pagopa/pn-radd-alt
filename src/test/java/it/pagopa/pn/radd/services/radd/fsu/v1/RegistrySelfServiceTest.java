package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.NormalizedAddressMapper;
import it.pagopa.pn.radd.mapper.RaddRegistryMapper;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.utils.OpeningHoursParser;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {RegistrySelfService.class})
@CustomLog
class RegistrySelfServiceTest {

    @Mock
    private RaddRegistryV2DAO raddRegistryDAO;
    private RegistrySelfService registrySelfService;

    private static final String PATTERN_FORMAT = "yyyy-MM-dd";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

    private final String PARTNER_ID = "partnerId";
    private final String LOCATION_ID = "locationId";

    private final String OPENING_TIME_Ok1 = """
                                              Lun-Ven 08:00-12:00, 14:00-18:00
                                              Sab 09:00-12:00
                                              Dom 10:00-11:00
                                              """;

    private final String OPENING_TIME_Ok2 = """
            Lun 09:00-13:00, 14:00-18:00; mar 09:30-12:30; MER 09:00-18:00
            """;

    private final String OPENING_TIME_Ko = """
            Lun-Ven 08:00-12:00
            Dom off
            """;

    @BeforeEach
    void setUp() {
        registrySelfService = new RegistrySelfService(raddRegistryDAO,new RaddRegistryMapper(new NormalizedAddressMapper()));
    }

    private UpdateRegistryRequestV2 updateRegistryRequestV2() {
        UpdateRegistryRequestV2 request = new UpdateRegistryRequestV2();

        Instant now = Instant.now();
        formatter.format(now);
        request.setEndValidity(formatter.format(now.plus(1, ChronoUnit.DAYS)));
        request.setDescription("description");
        request.setPhoneNumbers(List.of("+390123456789"));
        request.setExternalCodes(List.of("EXT0"));
        request.setEmail("mail@esempio.it");
        request.setOpeningTime(OPENING_TIME_Ok1);
        request.setAppointmentRequired(true);
        request.setWebsite("https://test.it");
        return request;
    }

    @Test
    void updateRegistry() {
        UpdateRegistryRequestV2 request = updateRegistryRequestV2();

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        Instant now = Instant.now();
        entity.setStartValidity(now);
        entity.setPartnerId(PARTNER_ID);
        entity.setLocationId(LOCATION_ID);

        when(raddRegistryDAO.findByPartnerId(PARTNER_ID)).thenReturn(Flux.empty());
        when(raddRegistryDAO.find(PARTNER_ID, LOCATION_ID)).thenReturn(Mono.just(entity));
        when(raddRegistryDAO.updateRegistryEntity(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(registrySelfService.updateRegistry(PARTNER_ID, LOCATION_ID, request))
                    .expectNextMatches(raddRegistryEntity -> entity.getDescription().equalsIgnoreCase(request.getDescription())
                                                             && entity.getEmail().equalsIgnoreCase(request.getEmail()))
                    .verifyComplete();
    }

    @Test
    void updateRegistry_NotFound() {
        UpdateRegistryRequestV2 request = updateRegistryRequestV2();

        when(raddRegistryDAO.find(PARTNER_ID, LOCATION_ID)).thenReturn(Mono.empty());

        StepVerifier.create(registrySelfService.updateRegistry(PARTNER_ID, LOCATION_ID, new UpdateRegistryRequestV2()))
                    .verifyErrorMessage(ExceptionTypeEnum.RADD_REGISTRY_NOT_FOUND.getMessage());
    }

    @Test
    void updateRegistry_DuplicatedExternalCode() {
        UpdateRegistryRequestV2 request = updateRegistryRequestV2();

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        entity.setLocationId(UUID.randomUUID().toString());
        entity.setExternalCodes(List.of("EXT1", "EXT2", "EXT3"));

        when(raddRegistryDAO.findByPartnerId(PARTNER_ID)).thenReturn(Flux.just(entity));
        when(raddRegistryDAO.find(PARTNER_ID, LOCATION_ID)).thenReturn(Mono.just(entity));

        request.setExternalCodes(List.of("EXT1"));
        StepVerifier.create(registrySelfService.updateRegistry(PARTNER_ID, LOCATION_ID, request))
                    .expectErrorMatches(throwable -> throwable instanceof RaddGenericException &&
                                                     ((RaddGenericException) throwable).getExceptionType() == ExceptionTypeEnum.DUPLICATE_EXT_CODE)
                    .verify();
    }

    @Test
    void updateRegistry_ValidOpeningTime1() {
        UpdateRegistryRequestV2 request = updateRegistryRequestV2();
        request.setOpeningTime(OPENING_TIME_Ok1);

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        Instant now = Instant.now();
        entity.setStartValidity(now);
        entity.setPartnerId(PARTNER_ID);
        entity.setLocationId(LOCATION_ID);

        when(raddRegistryDAO.findByPartnerId(PARTNER_ID)).thenReturn(Flux.empty());
        when(raddRegistryDAO.find(PARTNER_ID, LOCATION_ID)).thenReturn(Mono.just(entity));
        when(raddRegistryDAO.updateRegistryEntity(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(registrySelfService.updateRegistry(PARTNER_ID, LOCATION_ID, request))
                    .expectNextMatches(raddRegistryEntity -> entity.getDescription().equalsIgnoreCase(request.getDescription())
                                                             && entity.getEmail().equalsIgnoreCase(request.getEmail()))
                    .verifyComplete();
    }

    @Test
    void updateRegistry_ValidOpeningTime2() {
        UpdateRegistryRequestV2 request = updateRegistryRequestV2();
        request.setOpeningTime(OPENING_TIME_Ok2);

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        Instant now = Instant.now();
        entity.setStartValidity(now);
        entity.setPartnerId(PARTNER_ID);
        entity.setLocationId(LOCATION_ID);

        when(raddRegistryDAO.findByPartnerId(PARTNER_ID)).thenReturn(Flux.empty());
        when(raddRegistryDAO.find(PARTNER_ID, LOCATION_ID)).thenReturn(Mono.just(entity));
        when(raddRegistryDAO.updateRegistryEntity(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(registrySelfService.updateRegistry(PARTNER_ID, LOCATION_ID, request))
                    .expectNextMatches(raddRegistryEntity -> entity.getDescription().equalsIgnoreCase(request.getDescription())
                                                             && entity.getEmail().equalsIgnoreCase(request.getEmail()))
                    .verifyComplete();
    }

    /*
    @Test
    void updateRegistry_InvalidOpeningTime() {
        UpdateRegistryRequestV2 request = updateRegistryRequestV2();
        request.setOpeningTime(OPENING_TIME_Ko);

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        Instant now = Instant.now();
        entity.setStartValidity(now);
        entity.setPartnerId(PARTNER_ID);
        entity.setLocationId(LOCATION_ID);

        when(raddRegistryDAO.findByPartnerId(PARTNER_ID)).thenReturn(Flux.empty());
        when(raddRegistryDAO.find(PARTNER_ID, LOCATION_ID)).thenReturn(Mono.just(entity));
        when(raddRegistryDAO.updateRegistryEntity(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(registrySelfService.updateRegistry(PARTNER_ID, LOCATION_ID, request))
                    .expectErrorMatches(throwable -> throwable instanceof RaddGenericException &&
                                                     ((RaddGenericException) throwable).getExceptionType().getMessage().equals("Formato Opening Hours non valido"))
                    .verify();
    }

    @Test
    void updateRegistry_InvalidOpeningTime() {
        UpdateRegistryRequestV2 request = updateRegistryRequestV2();
        request.setOpeningTime(OPENING_TIME_Ko);

        StepVerifier.create(registrySelfService.updateRegistry(PARTNER_ID, LOCATION_ID, request))
                    .expectErrorMatches(throwable -> throwable instanceof RaddGenericException &&
                                                     ((RaddGenericException) throwable).getExceptionType() == ExceptionTypeEnum.OPENING_TIME_ERROR)
                    .verify();
    }
    */

}
package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.RaddRegistryMapper;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.*;
import it.pagopa.pn.radd.utils.OpeningHoursParser;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static it.pagopa.pn.radd.utils.DateUtils.convertDateToInstantAtStartOfDay;
import static it.pagopa.pn.radd.utils.DateUtils.getStartOfDayToday;

@Service
@RequiredArgsConstructor
@CustomLog
public class RegistrySelfService {

    private final RaddRegistryV2DAO raddRegistryDAO;
    private  final RaddRegistryMapper raddRegistryMapper;

    public Mono<RegistryV2> updateRegistry(String partnerId, String locationId, UpdateRegistryRequestV2 request) {
        checkUpdateRegistryRequest(request);
        log.info("Start updateRegistry for partnerId [{}] and locationId [{}]", partnerId, locationId);
        return validateExternalCodes(partnerId, locationId, request.getExternalCodes())
                .then(raddRegistryDAO.find(partnerId, locationId))
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.RADD_REGISTRY_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(registryEntity -> raddRegistryDAO.updateRegistryEntity(mapFieldToUpdate(registryEntity, request)))
                .map(raddRegistryMapper::toDto)
                .doOnError(throwable -> log.error("Error during update registry request for partnerId: [{}] and locationId: [{}]", partnerId, locationId, throwable));
    }

    private void checkUpdateRegistryRequest(UpdateRegistryRequestV2 request) {
        if (StringUtils.isNotBlank(request.getOpeningTime())) {
            if (!OpeningHoursParser.isValidOpenHours(request.getOpeningTime())) {
                throw new RaddGenericException(ExceptionTypeEnum.OPENING_TIME_ERROR, HttpStatus.BAD_REQUEST);
            }
        }
    }

    private RaddRegistryEntityV2 mapFieldToUpdate(RaddRegistryEntityV2 registryEntity, UpdateRegistryRequestV2 request) {
        if (StringUtils.isNotBlank(request.getDescription())) {
            registryEntity.setDescription(request.getDescription());
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            registryEntity.setEmail(request.getEmail());
        }

        if (StringUtils.isNotBlank(request.getOpeningTime())) {
            registryEntity.setOpeningTime(request.getOpeningTime());
        }
        if (!CollectionUtils.isEmpty(request.getExternalCodes())) {
            registryEntity.setExternalCodes(request.getExternalCodes());
        }
        if (!CollectionUtils.isEmpty(request.getPhoneNumbers())) {
            registryEntity.setPhoneNumbers(request.getPhoneNumbers());
        }

        if (StringUtils.isNotBlank(request.getWebsite())) {
            registryEntity.setWebsite(request.getWebsite());
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            registryEntity.setEmail(request.getEmail());
        }

        if (request.getAppointmentRequired() != null) {
            registryEntity.setAppointmentRequired(request.getAppointmentRequired());
        }

        if (request.getEndValidity() != null) {
            registryEntity.setEndValidity(verifyDatesForUpdate(registryEntity.getStartValidity(), request.getEndValidity()));
        }

        return registryEntity;
    }

    private Instant verifyDatesForUpdate(Instant startValidity, String endValidity) {
        Instant endValidityInstant = null;
        try {
            Instant startValidityInstant = startValidity != null ? startValidity : getStartOfDayToday();
            endValidityInstant = convertDateToInstantAtStartOfDay(endValidity);
            if (endValidityInstant.isBefore(startValidityInstant)) {
                throw new RaddGenericException(ExceptionTypeEnum.DATE_INTERVAL_ERROR, HttpStatus.BAD_REQUEST);
            }
        } catch (DateTimeParseException e) {
            throw new RaddGenericException(ExceptionTypeEnum.DATE_INVALID_ERROR, HttpStatus.BAD_REQUEST);
        }
        return endValidityInstant;
    }

    private Mono<Void> validateExternalCodes(String partnerId, String locationId, List<String> externalCodes) {
        if (externalCodes == null || externalCodes.isEmpty()) {
            return Mono.empty();
        }

        return raddRegistryDAO.findByPartnerId(partnerId)
                              .filter(entity -> !entity.getLocationId().equals(locationId))
                              .flatMap(entity ->
                                               Flux.fromIterable(entity.getExternalCodes())
                                                   .filter(externalCodes::contains)
                                                   .next()
                                      )
                              .filter(externalCodes::contains)
                              .next() // Prende il primo codice esterno duplicato trovato, se esiste
                              .flatMap(duplicate -> Mono.error(new RaddGenericException(ExceptionTypeEnum.DUPLICATE_EXT_CODE,
                                                                                        String.format("L'externalCode '%s' è già associato ad un'altra sede", duplicate),
                                                                                        HttpStatus.CONFLICT)))
                              .then();
    }

}
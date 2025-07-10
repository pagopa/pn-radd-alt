package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.exception.TransactionAlreadyExistsException;
import it.pagopa.pn.radd.mapper.RaddRegistryMapper;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static it.pagopa.pn.radd.utils.DateUtils.convertDateToInstantAtStartOfDay;
import static it.pagopa.pn.radd.utils.DateUtils.getStartOfDayToday;
import static it.pagopa.pn.radd.utils.OpeningHoursParser.validateOpenHours;
import static it.pagopa.pn.radd.utils.RaddRegistryUtils.buildRaddRegistryEntity;

@Service
@RequiredArgsConstructor
@CustomLog
public class RegistrySelfService {

    private final RaddRegistryV2DAO raddRegistryDAO;
    private final AwsGeoService awsGeoService;
    private  final RaddRegistryMapper raddRegistryMapper;

    public Mono<RegistryV2> addRegistry(String partnerId, String locationId, String uid, CreateRegistryRequestV2 request) {
        checkCreateRegistryRequest(request);
        log.info("Creating registry entity for partnerId: {} and locationId: {}", partnerId, locationId);
        AddressV2 inputAddress = request.getAddress();
        return validateExternalCodes(partnerId, locationId, request.getExternalCodes())
                .then(Mono.defer(() -> awsGeoService.getCoordinatesForAddress(
                        inputAddress.getAddressRow(),
                        inputAddress.getProvince(),
                        inputAddress.getCap(),
                        inputAddress.getCity()))
                )
                .map(coordinatesResult -> buildRaddRegistryEntity(partnerId, locationId, uid, request, coordinatesResult))
                .flatMap(raddRegistryDAO::putItemIfAbsent)
                .doOnNext(result -> log.debug("Registry entity with partnerId: {} and locationId: {} created successfully", partnerId, locationId))
                .map(raddRegistryMapper::toDto);
    }

    private void checkCreateRegistryRequest(CreateRegistryRequestV2 request) {
        verifyDates(request.getStartValidity(), request.getEndValidity());
        validateOpenHours(request.getOpeningTime());
    }

    private void verifyDates(String startValidity, String endValidity) {
        try {
            Instant today = getStartOfDayToday();
            Instant startValidityInstant = startValidity != null ? convertDateToInstantAtStartOfDay(startValidity) : today;

            if (startValidityInstant.isBefore(today)) {
                throw new RaddGenericException(ExceptionTypeEnum.START_VALIDITY_IN_THE_PAST, HttpStatus.BAD_REQUEST);
            }

            if (endValidity != null) {
                Instant endValidityInstant = convertDateToInstantAtStartOfDay(endValidity);
                if (endValidityInstant.isBefore(startValidityInstant)) {
                    throw new RaddGenericException(ExceptionTypeEnum.DATE_INTERVAL_ERROR, HttpStatus.BAD_REQUEST);
                }
            }
        } catch (DateTimeParseException e) {
            throw new RaddGenericException(ExceptionTypeEnum.DATE_INVALID_ERROR, HttpStatus.BAD_REQUEST);
        }
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

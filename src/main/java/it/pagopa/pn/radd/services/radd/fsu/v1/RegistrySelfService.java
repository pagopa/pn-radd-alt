package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.RaddRegistryMapper;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import static it.pagopa.pn.radd.utils.DateUtils.convertDateToInstantAtStartOfDay;
import static it.pagopa.pn.radd.utils.DateUtils.getStartOfDayToday;

@Service
@RequiredArgsConstructor
@CustomLog
public class RegistrySelfService {

    private final RaddRegistryV2DAO raddRegistryDAO;
    private final AwsGeoService awsGeoService;
    private  final RaddRegistryMapper raddRegistryMapper;

    public Mono<RegistryV2> addRegistry(String partnerId, String locationId, CreateRegistryRequestV2 request) {
        checkCreateRegistryRequest(request);
        log.info("Creating registry entity for partnerId: {} and locationId: {}", partnerId, locationId);
        AddressV2 inputAddress = request.getAddress();
        return awsGeoService.getCoordinatesForAddress(inputAddress.getAddressRow(), inputAddress.getProvince(), inputAddress.getCap(), inputAddress.getCity())
                .map(coordinatesResult -> {
                    RaddRegistryEntityV2 raddRegistryEntityV2 = new RaddRegistryEntityV2();

                    raddRegistryEntityV2.setPartnerId(partnerId);
                    raddRegistryEntityV2.setLocationId(locationId);
                    raddRegistryEntityV2.setDescription(request.getDescription());
                    raddRegistryEntityV2.setPhoneNumbers(request.getPhoneNumbers());
                    raddRegistryEntityV2.setOpeningTime(request.getOpeningTime());
                    raddRegistryEntityV2.setCapacity(request.getCapacity());
                    raddRegistryEntityV2.setExternalCodes(request.getExternalCodes());
                    raddRegistryEntityV2.setStartValidity(request.getStartValidity() != null ? convertDateToInstantAtStartOfDay(request.getStartValidity()) : getStartOfDayToday());
                    raddRegistryEntityV2.setEndValidity(convertDateToInstantAtStartOfDay(request.getEndValidity()));
                    raddRegistryEntityV2.setEmail(request.getEmail());
                    raddRegistryEntityV2.setAppointmentRequired(request.getAppointmentRequired());
                    raddRegistryEntityV2.setWebsite(request.getWebsite());
                    raddRegistryEntityV2.setPartnerType(request.getPartnerType());
                    raddRegistryEntityV2.setCreationTimestamp(Instant.now());
                    raddRegistryEntityV2.setUpdateTimestamp(Instant.now());

                    NormalizedAddressEntity normalizedAddress = new NormalizedAddressEntity();
                    normalizedAddress.setAddressRow(coordinatesResult.awsAddressRow);
                    normalizedAddress.setCity(coordinatesResult.awsLocality);
                    normalizedAddress.setCap(coordinatesResult.awsPostalCode);
                    normalizedAddress.setProvince(coordinatesResult.awsSubRegion);
                    normalizedAddress.setCountry(coordinatesResult.awsCountry);
                    normalizedAddress.setBiasPoint(coordinatesResult.biasPoint);
                    normalizedAddress.setLongitude(coordinatesResult.awsLongitude);
                    normalizedAddress.setLatitude(coordinatesResult.awsLatitude);

                    AddressEntity address = new AddressEntity();
                    address.setAddressRow(inputAddress.getAddressRow());
                    address.setCity(inputAddress.getCity());
                    address.setCap(inputAddress.getCap());
                    address.setProvince(inputAddress.getProvince());
                    address.setCountry(inputAddress.getCountry());

                    raddRegistryEntityV2.setNormalizedAddress(normalizedAddress);
                    raddRegistryEntityV2.setAddress(address);

                    return raddRegistryEntityV2;
                })
                .flatMap(raddRegistryDAO::putItemIfAbsent)
                .doOnNext(result -> log.debug("Registry entity with partnerId: {} and locationId: {} created successfully", partnerId, locationId))
                .map(raddRegistryMapper::toDto);
    }

    private void checkCreateRegistryRequest(CreateRegistryRequestV2 request) {
        verifyDates(request.getStartValidity(), request.getEndValidity());
        // TODO Da aggiungere controllo su openingTime e sull'univocità degli externalCodes
    }

    private void verifyDates(String startValidity, String endValidity) {
        try {
            Instant today = getStartOfDayToday();
            Instant startValidityInstant = startValidity != null ? convertDateToInstantAtStartOfDay(startValidity) : today;

            if (startValidityInstant.isBefore(today)) {
                throw new RaddGenericException(ExceptionTypeEnum.DATE_IN_THE_PAST, HttpStatus.BAD_REQUEST);
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

}

package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateRegistryRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CreateRegistryRequestGeoLocation;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryImportEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryRequestEntity;
import it.pagopa.pn.radd.pojo.RaddRegistryOriginalRequest;
import it.pagopa.pn.radd.pojo.RaddRegistryRequest;
import it.pagopa.pn.radd.pojo.RegistryRequestStatus;
import it.pagopa.pn.radd.utils.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static it.pagopa.pn.radd.utils.Const.MISSING_ADDRESS_REQUIRED_FIELD;
import static it.pagopa.pn.radd.utils.DateUtils.convertDateToInstantAtStartOfDay;

@RequiredArgsConstructor
@Component
public class RaddRegistryRequestEntityMapper {

    private final ObjectMapperUtil objectMapperUtil;

    public RaddRegistryOriginalRequest retrieveOriginalRequest(CreateRegistryRequest request) {
        RaddRegistryOriginalRequest originalRequest = new RaddRegistryOriginalRequest();
        if(request.getAddress() != null) {
            originalRequest.setAddressRow(request.getAddress().getAddressRow());
            originalRequest.setCap(request.getAddress().getCap());
            originalRequest.setCity(request.getAddress().getCity());
            originalRequest.setPr(request.getAddress().getPr());
            originalRequest.setCountry(request.getAddress().getCountry());
        }
        if(request.getStartValidity() != null ) {
            Instant instant = convertDateToInstantAtStartOfDay(request.getStartValidity());

            originalRequest.setStartValidity(instant.toString());
        } else {
            originalRequest.setStartValidity(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toString());
        }

        if(request.getEndValidity() != null) {
            Instant instant = convertDateToInstantAtStartOfDay(request.getEndValidity());
            originalRequest.setEndValidity(instant.toString());
        }

        originalRequest.setOpeningTime(request.getOpeningTime());
        originalRequest.setDescription(request.getDescription());
        if(request.getGeoLocation() != null) {
            originalRequest.setGeoLocation(objectMapperUtil.toJson(request.getGeoLocation()));
        }
        originalRequest.setPhoneNumber(request.getPhoneNumber());
        originalRequest.setExternalCode(request.getExternalCode());
        originalRequest.setCapacity(request.getCapacity());

        return originalRequest;
    }

    public RaddRegistryRequestEntity retrieveRaddRegistryRequestEntity(String cxId, String requestId, RaddRegistryOriginalRequest originalRequest) {

        String originalRequestString = objectMapperUtil.toJson(originalRequest);

        RaddRegistryRequestEntity requestEntity = new RaddRegistryRequestEntity();
        requestEntity.setPk(buildPk(cxId, requestId, originalRequestString));
        requestEntity.setCxId(cxId);
        requestEntity.setRequestId(requestId);
        requestEntity.setCorrelationId(requestId);
        requestEntity.setCreatedAt(Instant.now());
        requestEntity.setUpdatedAt(Instant.now());
        requestEntity.setOriginalRequest(originalRequestString);
        requestEntity.setStatus(RegistryRequestStatus.NOT_WORKED.name());
        return requestEntity;
    }
    private static String buildPk(String cxId, String requestId, String originalRequest) {
        UUID index = UUID.nameUUIDFromBytes(originalRequest.getBytes(StandardCharsets.UTF_8));
        return cxId + "#" + requestId + "#" + index;
    }

    public List<RaddRegistryOriginalRequest> retrieveOriginalRequest(List<RaddRegistryRequest> raddRegistryRequest) {

        return raddRegistryRequest.stream()
                .map(this::mapRequestToOriginalRequest)
                .toList();
    }

    private CreateRegistryRequestGeoLocation retrieveGeoLocationObject(String coordinateGeoReferenziali) {
        String[] coordinates = coordinateGeoReferenziali.split(",");
        if (coordinates.length != 2) {
            return null;
        }
        CreateRegistryRequestGeoLocation geoLocation = new CreateRegistryRequestGeoLocation();
        geoLocation.setLatitude(coordinates[0]);
        geoLocation.setLongitude(coordinates[1]);
        return geoLocation;
    }

    public List<RaddRegistryRequestEntity> retrieveRaddRegistryRequestEntity(List<RaddRegistryOriginalRequest> originalRequests, RaddRegistryImportEntity importEntity) {
        return originalRequests.stream().map(getRaddRegistryOriginalRequestRaddRegistryRequestEntity(importEntity)).toList();
    }

    private Function<RaddRegistryOriginalRequest, RaddRegistryRequestEntity> getRaddRegistryOriginalRequestRaddRegistryRequestEntity(RaddRegistryImportEntity importEntity) {
        return originalRequest -> {
            String originalRequestString = objectMapperUtil.toJson(originalRequest);

            RaddRegistryRequestEntity requestEntity = new RaddRegistryRequestEntity();
            requestEntity.setPk(buildPk(importEntity, originalRequestString));
            requestEntity.setCxId(importEntity.getCxId());
            requestEntity.setRequestId(importEntity.getRequestId());
            requestEntity.setCreatedAt(Instant.now());
            requestEntity.setUpdatedAt(Instant.now());
            requestEntity.setOriginalRequest(originalRequestString);

            checkRequiredFieldsAndSetStatus(originalRequest, requestEntity);

            return requestEntity;
        };
    }

    private void checkRequiredFieldsAndSetStatus(RaddRegistryOriginalRequest originalRequest, RaddRegistryRequestEntity requestEntity) {
        if (StringUtils.isBlank(originalRequest.getAddressRow())
                || StringUtils.isBlank(originalRequest.getCap())
                || StringUtils.isBlank(originalRequest.getCity())
                || StringUtils.isBlank(originalRequest.getPr())) {
            requestEntity.setStatus(RegistryRequestStatus.REJECTED.name());
            requestEntity.setError(MISSING_ADDRESS_REQUIRED_FIELD);
        } else {
            requestEntity.setStatus(RegistryRequestStatus.NOT_WORKED.name());
        }
    }

    private static String buildPk(RaddRegistryImportEntity importEntity, String originalRequest) {
        UUID index = UUID.nameUUIDFromBytes(originalRequest.getBytes(StandardCharsets.UTF_8));
        return importEntity.getCxId() + "#" + importEntity.getRequestId() + "#" + index;
    }
    private String convertDate(String date) {
        LocalDate localDate = LocalDate.parse(date);
        Instant instant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        return instant.toString();
    }
    private RaddRegistryOriginalRequest mapRequestToOriginalRequest(RaddRegistryRequest request) {
        RaddRegistryOriginalRequest originalRequest = new RaddRegistryOriginalRequest();
        mapAddressForOriginalRequest(request, originalRequest);
        setValidityDatesForOriginalRequest(request, originalRequest);
        originalRequest.setOpeningTime(request.getOrariApertura());
        originalRequest.setDescription(request.getDescrizione());
        if (StringUtils.isNotBlank(request.getCoordinateGeoReferenziali())) {
            originalRequest.setGeoLocation(objectMapperUtil.toJson(retrieveGeoLocationObject(request.getCoordinateGeoReferenziali())));
        }
        originalRequest.setPhoneNumber(request.getTelefono());
        originalRequest.setExternalCode(request.getExternalCode());
        originalRequest.setCapacity(request.getCapacita());

        return originalRequest;
    }
    private void setValidityDatesForOriginalRequest(RaddRegistryRequest request, RaddRegistryOriginalRequest originalRequest) {
        if (StringUtils.isNotBlank(request.getDataInizioValidita())) {
            originalRequest.setStartValidity(convertDate(request.getDataInizioValidita()));
        } else {
            originalRequest.setStartValidity(convertDate(LocalDate.now().toString()));
        }

        if (StringUtils.isNotBlank(request.getDataFineValidita())) {
            originalRequest.setEndValidity(convertDate(request.getDataFineValidita()));
        }
    }
    private void mapAddressForOriginalRequest(RaddRegistryRequest request, RaddRegistryOriginalRequest originalRequest) {
        originalRequest.setAddressRow(request.getVia());
        originalRequest.setCap(request.getCap());
        originalRequest.setCity(request.getCitta());
        originalRequest.setPr(request.getProvincia());
        originalRequest.setCountry(request.getPaese());
    }
}

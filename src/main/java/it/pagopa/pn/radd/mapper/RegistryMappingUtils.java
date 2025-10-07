package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Address;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.GeoLocation;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Registry;
import it.pagopa.pn.radd.middleware.db.entities.*;
import it.pagopa.pn.radd.pojo.RaddRegistryOriginalRequest;
import it.pagopa.pn.radd.services.radd.fsu.v1.AwsGeoService;
import it.pagopa.pn.radd.utils.Const;
import it.pagopa.pn.radd.utils.ObjectMapperUtil;
import it.pagopa.pn.radd.utils.RaddRegistryUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static it.pagopa.pn.radd.utils.RaddRegistryUtils.areAddressesEquivalent;

@Component
@CustomLog
@RequiredArgsConstructor
public class RegistryMappingUtils {

    private final ObjectMapperUtil objectMapperUtil;

    public Registry mappingToV1(RaddRegistryEntityV2 v2) {
        Registry v1 = new Registry();

        if (v2 == null) {
            return null;
        }

        v1.setRegistryId(v2.getLocationId());
        //phoneNumbers per v2 è una lista
        if(null != v2.getPhoneNumbers() && !v2.getPhoneNumbers().isEmpty()){
            v1.setPhoneNumber(v2.getPhoneNumbers().get(0));
        }
        //externalCode per v2 è una lista
        if(null!= v2.getExternalCodes() && !v2.getExternalCodes().isEmpty()){
            v1.setExternalCode(v2.getExternalCodes().get(0));
        }
        v1.setDescription(v2.getDescription());
        v1.setOpeningTime(v2.getOpeningTime());

        if (v2.getStartValidity() != null){
            v1.setStartValidity(Date.from(v2.getStartValidity()));
        }
        if (v2.getEndValidity() != null) {
            v1.setEndValidity(Date.from(v2.getEndValidity()));
        }
        //mapping Address
        Address address = new Address();
        address.setAddressRow(v2.getNormalizedAddress().getAddressRow());
        address.setCap(v2.getNormalizedAddress().getCap());
        address.setCity(v2.getNormalizedAddress().getCity());
        address.setPr(v2.getNormalizedAddress().getProvince());
        address.setCountry(v2.getNormalizedAddress().getCountry());
        v1.setAddress(address);


        return v1;
    }
    //utilizzato nella creazione
    public RaddRegistryEntityV2 mappingToV2(
            String registryId,
            String uid,
            AwsGeoService.CoordinatesResult coordinatesResult,
            RaddRegistryRequestEntity registryRequest,
            RaddRegistryOriginalRequest originalRequest) {

        if (registryRequest == null || originalRequest == null || coordinatesResult == null) {
            return null;
        }

        RaddRegistryEntityV2 v2 = buildCommonFields(uid, originalRequest);

        v2.setLocationId(registryId);
        v2.setPartnerId(registryRequest.getCxId());
        v2.setRequestId(registryRequest.getRequestId());

        NormalizedAddressEntityV2 normalizedAddress = RaddRegistryUtils.buildNormalizedAddressEntity(coordinatesResult);
        AddressEntity addressV2 = new AddressEntity();
        addressV2.setAddressRow(originalRequest.getAddressRow());
        addressV2.setCap(originalRequest.getCap());
        addressV2.setCity(originalRequest.getCity());
        addressV2.setProvince(originalRequest.getPr());
        addressV2.setCountry(originalRequest.getCountry());

        v2.setAddress(addressV2);
        v2.setNormalizedAddress(normalizedAddress);
        v2.setModifiedAddress(!areAddressesEquivalent(addressV2, normalizedAddress));

        return v2;
    }
    //utilizzato nell'update
    public RaddRegistryEntityV2 mappingToV2(
            String uid,
            RaddRegistryEntityV2 preExistingRegistryEntity,
            RaddRegistryRequestEntity registryRequest,
            RaddRegistryOriginalRequest originalRequest) {

        if (registryRequest == null || originalRequest == null || preExistingRegistryEntity == null) {
            return null;
        }

        RaddRegistryEntityV2 v2 = buildCommonFields(uid, originalRequest);

        v2.setLocationId(preExistingRegistryEntity.getLocationId());
        v2.setPartnerId(preExistingRegistryEntity.getPartnerId());

        // Reuse existing addresses
        v2.setAddress(preExistingRegistryEntity.getAddress());
        v2.setNormalizedAddress(preExistingRegistryEntity.getNormalizedAddress());

        return v2;
    }

    private RaddRegistryEntityV2 buildCommonFields(
            String uid,
            RaddRegistryOriginalRequest originalRequest) {

        RaddRegistryEntityV2 v2 = new RaddRegistryEntityV2();

        v2.setPhoneNumbers(originalRequest.getPhoneNumber() != null ? List.of(originalRequest.getPhoneNumber()) : List.of());
        v2.setExternalCodes(originalRequest.getExternalCode() != null ? List.of(originalRequest.getExternalCode()) : List.of());
        v2.setDescription(originalRequest.getDescription());
        v2.setOpeningTime(originalRequest.getOpeningTime());
        v2.setUid(uid);

        if (StringUtils.isNotBlank(originalRequest.getStartValidity())) {
            v2.setStartValidity(Instant.parse(originalRequest.getStartValidity()));
        }

        if (StringUtils.isNotBlank(originalRequest.getEndValidity())) {
            v2.setEndValidity(Instant.parse(originalRequest.getEndValidity()));
        }

        v2.setPartnerType(Const.PARTNERTYPE_CAF);
        return v2;
    }

}

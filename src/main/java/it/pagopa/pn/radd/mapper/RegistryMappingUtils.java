package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.GeoLocation;
import it.pagopa.pn.radd.middleware.db.entities.NormalizedAddressEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.utils.Const;
import it.pagopa.pn.radd.utils.ObjectMapperUtil;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@CustomLog
@RequiredArgsConstructor
public class RegistryMappingUtils {

    private final ObjectMapperUtil objectMapperUtil;

    public RaddRegistryEntity mappingToV1(RaddRegistryEntityV2 v2, String partnerId) {
        RaddRegistryEntity v1 = new RaddRegistryEntity();

        if (v2 == null) {
            return null;
        }

        v1.setCxId(partnerId);
        v1.setRegistryId(v2.getLocationId());
        //phoneNumbers per v2 è una lista
        v1.setPhoneNumber(v2.getPhoneNumbers().get(0));
        //externalCode per v2 è una lista
        v1.setExternalCode(v2.getExternalCodes().get(0));

        v1.setDescription(v2.getDescription());
        v1.setOpeningTime(v2.getOpeningTime());
        v1.setStartValidity(v2.getStartValidity());
        v1.setEndValidity(v2.getEndValidity());
        v1.setZipCode(v2.getNormalizedAddress().getCap());

        //mapping Address
        NormalizedAddressEntity address = new NormalizedAddressEntity();
        address.setAddressRow(v2.getNormalizedAddress().getAddressRow());
        address.setCap(v2.getNormalizedAddress().getCap());
        address.setCity(v2.getNormalizedAddress().getCity());
        address.setProvince(v2.getNormalizedAddress().getProvince());
        address.setCountry(v2.getNormalizedAddress().getCountry());

        //geolocalizzazione
        address.setLatitude(v2.getNormalizedAddress().getLatitude());
        address.setLongitude(v2.getNormalizedAddress().getLongitude());
        v1.setNormalizedAddress(address);

        GeoLocation geoLocation = new GeoLocation();
        geoLocation.setLatitude(v2.getNormalizedAddress().getLatitude());
        geoLocation.setLongitude(v2.getNormalizedAddress().getLongitude());


        v1.setGeoLocation(objectMapperUtil.toJson(geoLocation));

        return v1;
    }

    public RaddRegistryEntityV2 mappingToV2(RaddRegistryEntity v1, String uid) {

        if (v1 == null || uid == null) {
            return null;
        }

        RaddRegistryEntityV2 v2 = new RaddRegistryEntityV2();

       v2.setLocationId(v1.getRegistryId());
       v2.setPartnerId(v1.getCxId());
       v2.setPhoneNumbers(List.of(v1.getPhoneNumber()));
       v2.setExternalCodes(List.of(v1.getExternalCode()));
       v2.setDescription(v1.getDescription());
       v2.setOpeningTime(v1.getOpeningTime());
       v2.setStartValidity(v1.getStartValidity());
       v2.setEndValidity(v1.getEndValidity());
       v2.setUid(uid); //lo prendiamo dall header

            //Mappiamo l'address
            NormalizedAddressEntity address = new NormalizedAddressEntity();
            address.setAddressRow(v1.getNormalizedAddress().getAddressRow());
            address.setCap(v1.getNormalizedAddress().getCap());
            address.setCity(v1.getNormalizedAddress().getCity());
            address.setProvince(v1.getNormalizedAddress().getProvince());
            address.setCountry(v1.getNormalizedAddress().getCountry());

            //geolocalizzazione
            address.setLatitude(v1.getNormalizedAddress().getLatitude());
            address.setLongitude(v1.getNormalizedAddress().getLongitude());
            v2.setNormalizedAddress(address);

        v2.setPartnerType(Const.PARTNERTYPE_CAF); //default a CAF

        return v2;
    }
}

package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.NormalizedAddress;
import it.pagopa.pn.radd.middleware.db.entities.NormalizedAddressEntity;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@CustomLog
public class NormalizedAddressMapper {

    public NormalizedAddressEntity toEntity(NormalizedAddress dto) {
        if (dto == null) {
            return null;
        }
        NormalizedAddressEntity entity = new NormalizedAddressEntity();
        entity.setAddressRow(dto.getAddressRow());
        entity.setCap(dto.getCap());
        entity.setCity(dto.getCity());
        entity.setProvince(dto.getProvince());
        entity.setCountry(dto.getCountry());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setBiasPoint(dto.getBiasPoint());
        return entity;
    }

    public NormalizedAddress toDto(NormalizedAddressEntity entity) {
        if (entity == null) {
            return null;
        }
        NormalizedAddress dto = new NormalizedAddress();
        dto.setAddressRow(entity.getAddressRow());
        dto.setCap(entity.getCap());
        dto.setCity(entity.getCity());
        dto.setProvince(entity.getProvince());
        dto.setCountry(entity.getCountry());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setBiasPoint(entity.getBiasPoint());
        return dto;
    }
}

package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.RegistryV2;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@CustomLog
public class RaddRegistryMapper {

    private final NormalizedAddressMapper normalizedAddressMapper;

    public RegistryV2 toDto(RaddRegistryEntityV2 entity) {
        if (entity == null) {
            return null;
        }

        RegistryV2 dto = new RegistryV2();
        dto.setPartnerId(entity.getPartnerId());
        dto.setLocationId(entity.getLocationId());
        dto.setExternalCodes(entity.getExternalCodes());
        dto.setPhoneNumbers(entity.getPhoneNumbers());
        dto.setEmail(entity.getEmail());
        dto.setAppointmentRequired(entity.getAppointmentRequired());
        dto.setWebsite(entity.getWebsite());
        dto.setPartnerType(entity.getPartnerType());
        dto.setCreationTimestamp(toDate(entity.getCreationTimestamp()));
        dto.setUpdateTimestamp(toDate(entity.getUpdateTimestamp()));
        dto.setDescription(entity.getDescription());
        dto.setOpeningTime(entity.getOpeningTime());
        dto.setStartValidity(toStringDate(entity.getStartValidity()));
        dto.setEndValidity(toStringDate(entity.getEndValidity()));
        dto.setCapacity(entity.getCapacity());
        dto.setNormalizedAddress(normalizedAddressMapper.toDto(entity.getNormalizedAddress()));

        return dto;
    }

    public RaddRegistryEntityV2 toEntity(RegistryV2 dto) {
        if (dto == null) {
            return null;
        }

        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        entity.setPartnerId(dto.getPartnerId());
        entity.setLocationId(dto.getLocationId());
        entity.setExternalCodes(dto.getExternalCodes());
        entity.setPhoneNumbers(dto.getPhoneNumbers());
        entity.setEmail(dto.getEmail());
        entity.setAppointmentRequired(dto.getAppointmentRequired());
        entity.setWebsite(dto.getWebsite());
        entity.setPartnerType(dto.getPartnerType());
        entity.setCreationTimestamp(toInstant(dto.getCreationTimestamp()));
        entity.setUpdateTimestamp(toInstant(dto.getUpdateTimestamp()));
        entity.setDescription(dto.getDescription());
        entity.setOpeningTime(dto.getOpeningTime());
        entity.setStartValidity(parseDateString(dto.getStartValidity()));
        entity.setEndValidity(parseDateString(dto.getEndValidity()));
        entity.setCapacity(dto.getCapacity());
        entity.setNormalizedAddress(normalizedAddressMapper.toEntity(dto.getNormalizedAddress()));

        return entity;
    }

    private Instant toInstant(Date date) {
        return Optional.ofNullable(date)
                .map(Date::toInstant)
                .orElse(null);
    }

    private Date toDate(Instant instant) {
        return Optional.ofNullable(instant)
                .map(Date::from)
                .orElse(null);
    }

    private String toStringDate(Instant instant) {
        return Optional.ofNullable(instant)
                .map(i -> OffsetDateTime.ofInstant(i, ZoneOffset.UTC).toLocalDate().toString())
                .orElse(null);
    }

    private Instant parseDateString(String dateStr) {
        try {
            return Optional.ofNullable(dateStr)
                    .map(LocalDate::parse)
                    .map(d -> d.atStartOfDay().toInstant(ZoneOffset.UTC))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

}

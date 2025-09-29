package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Coverage;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@CustomLog
public class CoverageMapper extends AbstractRegistryMapper {

    public Coverage toDto(CoverageEntity entity) {

        if (entity == null) {
            return null;
        }

        Coverage dto = new Coverage();
        dto.setCap(entity.getCap());
        dto.setLocality(entity.getLocality());
        dto.setProvince(entity.getProvince());
        dto.setCadastralCode(entity.getCadastralCode());
        dto.setStartValidity(entity.getStartValidity());
        dto.setEndValidity(entity.getEndValidity());

        return dto;
    }

    public CoverageEntity toEntity(Coverage dto) {

        if (dto == null) {
            return null;
        }

        CoverageEntity entity = new CoverageEntity();
        entity.setCap(dto.getCap());
        entity.setLocality(dto.getLocality());
        entity.setProvince(dto.getProvince());
        entity.setCadastralCode(dto.getCadastralCode());
        entity.setStartValidity(dto.getStartValidity());
        entity.setEndValidity(dto.getEndValidity());

        return entity;
    }

}
package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Coverage;
import it.pagopa.pn.radd.middleware.db.entities.CoverageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CoverageMapperTest {

    private CoverageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CoverageMapper();
    }

    private CoverageEntity buildEntity() {
        CoverageEntity entity = new CoverageEntity();
        entity.setCap("00043");
        entity.setLocality("Ciampino");
        entity.setProvince("RM");
        entity.setCadastralCode("M272");
        entity.setStartValidity(Date.from(Instant.now()));
        entity.setEndValidity(Date.from(Instant.now().plusSeconds(86400)));
        return entity;
    }

    private Coverage buildDto() {
        Coverage dto = new Coverage();
        dto.setCap("00043");
        dto.setLocality("Ciampino");
        dto.setProvince("RM");
        dto.setCadastralCode("M272");
        dto.setStartValidity(Date.from(Instant.now()));
        dto.setEndValidity(Date.from(Instant.now().plusSeconds(86400)));
        return dto;
    }

    @Test
    void testToDto_withValidEntity_shouldMapCorrectly() {
        CoverageEntity entity = buildEntity();

        Coverage dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals("00043", dto.getCap());
        assertEquals("Ciampino", dto.getLocality());
        assertEquals("RM", dto.getProvince());
        assertEquals("M272", dto.getCadastralCode());
    }

    @Test
    void testToDto_withNullEntity_shouldReturnNull() {
        Coverage dto = mapper.toDto(null);
        assertNull(dto);
    }

    @Test
    void testToEntity_withValidDto_shouldMapCorrectly() {
        Coverage dto = buildDto();

        CoverageEntity entity = mapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("00043", entity.getCap());
        assertEquals("Ciampino", entity.getLocality());
        assertEquals("RM", entity.getProvince());
        assertEquals("M272", entity.getCadastralCode());
    }

    @Test
    void testToEntity_withNullDto_shouldReturnNull() {
        CoverageEntity entity = mapper.toEntity(null);
        assertNull(entity);
    }
}
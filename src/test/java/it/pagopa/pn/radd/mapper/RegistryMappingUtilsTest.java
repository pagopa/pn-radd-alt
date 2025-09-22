package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.middleware.db.entities.NormalizedAddressEntity;
import it.pagopa.pn.radd.utils.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistryMappingUtilsTest {

    private RaddRegistryEntityV2 v2;
    private RaddRegistryEntity v1;
    private String uid;

    @BeforeEach
    void setUp() {
        uid = "UID123";
        v2 = new RaddRegistryEntityV2();
        v2.setLocationId("loc-123");
        v2.setPhoneNumbers(List.of("123456789"));
        v2.setExternalCodes(List.of("ext-001"));
        v2.setDescription("Test description");
        v2.setOpeningTime("08:00-18:00");
        v2.setStartValidity(Instant.parse("2025-01-01T00:00:00Z"));
        v2.setEndValidity(Instant.parse("2025-12-31T23:59:59Z"));

        NormalizedAddressEntity addressV2 = new NormalizedAddressEntity();
        addressV2.setAddressRow("Via Roma 1");
        addressV2.setCap("00100");
        addressV2.setCity("Roma");
        addressV2.setProvince("RM");
        addressV2.setCountry("Italia");
        addressV2.setLatitude("41.9028");
        addressV2.setLongitude("12.4964");
        v2.setNormalizedAddress(addressV2);

        v1 = new RaddRegistryEntity();
        v1.setRegistryId("loc-123");
        v1.setCxId("partner-001");
        v1.setPhoneNumber("123456789");
        v1.setExternalCode("ext-001");
        v1.setDescription("Test description");
        v1.setOpeningTime("08:00-18:00");
        v1.setStartValidity(Instant.parse("2025-01-01T00:00:00Z"));
        v1.setEndValidity(Instant.parse("2025-12-31T23:59:59Z"));

        NormalizedAddressEntity addressV1 = new NormalizedAddressEntity();
        addressV1.setAddressRow("Via Roma 1");
        addressV1.setCap("00100");
        addressV1.setCity("Roma");
        addressV1.setProvince("RM");
        addressV1.setCountry("Italia");
        addressV1.setLatitude("41.9028");
        addressV1.setLongitude("12.4964");
        v1.setNormalizedAddress(addressV1);
    }

    @Test
    void testMappingToV1_success() {
        RaddRegistryEntity v1 = RegistryMappingUtils.mappingToV1(v2);

        assertNotNull(v1);
        assertEquals("loc-123", v1.getRegistryId());
        assertEquals("123456789", v1.getPhoneNumber());
        assertEquals("ext-001", v1.getExternalCode());
        assertEquals("Test description", v1.getDescription());
        assertEquals("08:00-18:00", v1.getOpeningTime());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), v1.getStartValidity());
        assertEquals(Instant.parse("2025-12-31T23:59:59Z"), v1.getEndValidity());
        assertNotNull(v1.getNormalizedAddress());
        assertEquals("Via Roma 1", v1.getNormalizedAddress().getAddressRow());
        assertEquals("00100", v1.getNormalizedAddress().getCap());
        assertEquals("Roma", v1.getNormalizedAddress().getCity());
        assertEquals("RM", v1.getNormalizedAddress().getProvince());
        assertEquals("Italia", v1.getNormalizedAddress().getCountry());
        assertEquals("41.9028", v1.getNormalizedAddress().getLatitude());
        assertEquals("12.4964", v1.getNormalizedAddress().getLongitude());

        assertNull(v1.getCapacity(), "Capacity deve essere null perché in V2 non esiste");
    }

    @Test
    void testMappingToV1_withNullInput() {
        assertNull(RegistryMappingUtils.mappingToV1(null));
    }

    @Test
    void testMappingToV2_withNullInput() {
        assertNull(RegistryMappingUtils.mappingToV2(null, null));
    }

    @Test
    void testMappingToV1_withEmptyLists() {
        v2.setPhoneNumbers(List.of());
        v2.setExternalCodes(List.of());

        assertThrows(IndexOutOfBoundsException.class, () -> RegistryMappingUtils.mappingToV1(v2));
    }

    @Test
    void testMappingToV1_withEmptyAddress() {
        v2.setNormalizedAddress(new NormalizedAddressEntity());

        RaddRegistryEntity v1 = RegistryMappingUtils.mappingToV1(v2);
        assertNotNull(v1);
        assertNotNull(v1.getNormalizedAddress());
        assertNull(v1.getNormalizedAddress().getAddressRow());
        assertNull(v1.getNormalizedAddress().getCap());
        assertNull(v1.getNormalizedAddress().getCity());
        assertNull(v1.getNormalizedAddress().getProvince());
        assertNull(v1.getNormalizedAddress().getCountry());
        assertNull(v1.getNormalizedAddress().getLatitude());
        assertNull(v1.getNormalizedAddress().getLongitude());
    }

    @Test
    void testMappingToV2_success() {
        RaddRegistryEntityV2 v2Mapped = RegistryMappingUtils.mappingToV2(v1, uid);

        assertNotNull(v2Mapped);
        assertEquals("loc-123", v2Mapped.getLocationId());
        assertEquals("partner-001", v2Mapped.getPartnerId());
        assertEquals(List.of("123456789"), v2Mapped.getPhoneNumbers());
        assertEquals(List.of("ext-001"), v2Mapped.getExternalCodes());
        assertEquals("Test description", v2Mapped.getDescription());
        assertEquals("08:00-18:00", v2Mapped.getOpeningTime());
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), v2Mapped.getStartValidity());
        assertEquals(Instant.parse("2025-12-31T23:59:59Z"), v2Mapped.getEndValidity());
        assertEquals(uid, v2Mapped.getUid());
        assertNotNull(v2Mapped.getNormalizedAddress());
        assertEquals("Via Roma 1", v2Mapped.getNormalizedAddress().getAddressRow());
        assertEquals("00100", v2Mapped.getNormalizedAddress().getCap());
        assertEquals("Roma", v2Mapped.getNormalizedAddress().getCity());
        assertEquals("RM", v2Mapped.getNormalizedAddress().getProvince());
        assertEquals("Italia", v2Mapped.getNormalizedAddress().getCountry());
        assertEquals("41.9028", v2Mapped.getNormalizedAddress().getLatitude());
        assertEquals("12.4964", v2Mapped.getNormalizedAddress().getLongitude());
        assertFalse(v2Mapped.getAppointmentRequired());
        assertEquals(Const.PARTNERTYPE_CAF, v2Mapped.getPartnerType());
    }



    @Test
    void testMappingToV2_withEmptyStrings() {
        v1.setRegistryId("");
        v1.setCxId("");
        v1.setPhoneNumber("");
        v1.setExternalCode("");
        v1.setDescription("");
        v1.setOpeningTime("");

        RaddRegistryEntityV2 v2Mapped = RegistryMappingUtils.mappingToV2(v1, uid);

        assertNotNull(v2Mapped);
        assertEquals("", v2Mapped.getLocationId());
        assertEquals("", v2Mapped.getPartnerId());
        assertEquals(List.of(""), v2Mapped.getPhoneNumbers());
        assertEquals(List.of(""), v2Mapped.getExternalCodes());
        assertEquals("", v2Mapped.getDescription());
        assertEquals("", v2Mapped.getOpeningTime());
        assertEquals(uid, v2Mapped.getUid());
    }
}

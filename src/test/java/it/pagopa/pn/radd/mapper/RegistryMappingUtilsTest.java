package it.pagopa.pn.radd.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.Registry;
import it.pagopa.pn.radd.middleware.db.entities.*;
import it.pagopa.pn.radd.pojo.RaddRegistryOriginalRequest;
import it.pagopa.pn.radd.services.radd.fsu.v1.AwsGeoService;
import it.pagopa.pn.radd.utils.Const;
import it.pagopa.pn.radd.utils.ObjectMapperUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
//@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class RegistryMappingUtilsTest {

    private RaddRegistryEntityV2 v2;
    private RaddRegistryEntity v1;
    private String uid;
    private ObjectMapperUtil objectMapperUtil;
    private RegistryMappingUtils registryMappingUtils;
    private AddressEntity originalAddress;
    private String partnerId = "partner-001";
    private String registryId;
    private AwsGeoService.CoordinatesResult coordinateResult = new AwsGeoService.CoordinatesResult();
    private RaddRegistryRequestEntity registryRequest = new RaddRegistryRequestEntity();
    private RaddRegistryOriginalRequest originalRequest = new RaddRegistryOriginalRequest();

    @BeforeEach
    void setUp() {
        objectMapperUtil = new ObjectMapperUtil(new ObjectMapper());
        registryMappingUtils = new RegistryMappingUtils(objectMapperUtil);

        uid = "UID123";
        registryId = "loc-123";
        partnerId = "partner-001";

        // Costruzione v2
        v2 = buildRegistryEntityV2();

        // Costruzione v1
        v1 = buildRegistryEntityV1();

        // Request entities
        registryRequest = new RaddRegistryRequestEntity();
        registryRequest.setCxId(partnerId);
        registryRequest.setZipCode("00100");

        originalRequest = buildOriginalRequest();

        // Coordinate AWS
        coordinateResult = buildCoordinatesResult();
    }

    private RaddRegistryEntityV2 buildRegistryEntityV2() {
        RaddRegistryEntityV2 entity = new RaddRegistryEntityV2();
        entity.setLocationId(registryId);
        entity.setPartnerId(partnerId);
        entity.setPhoneNumbers(List.of("123456789"));
        entity.setExternalCodes(List.of("ext-001"));
        entity.setDescription("Test description");
        entity.setOpeningTime("08:00-18:00");
        entity.setStartValidity(Instant.parse("2025-01-01T00:00:00Z"));
        entity.setEndValidity(Instant.parse("2025-12-31T23:59:59Z"));
        entity.setUid(uid);
        entity.setPartnerType(Const.PARTNERTYPE_CAF);

        AddressEntity address = new AddressEntity();
        address.setCap("00100");
        address.setCity("Roma");
        address.setProvince("RM");
        address.setCountry("Italia");
        address.setAddressRow("Via Roma 1");
        entity.setAddress(address);

        NormalizedAddressEntityV2 normalized = new NormalizedAddressEntityV2();
        normalized.setAddressRow("Via Roma 1");
        normalized.setCap("00100");
        normalized.setCity("Roma");
        normalized.setProvince("RM");
        normalized.setCountry("Italia");
        normalized.setLatitude("41.9028");
        normalized.setLongitude("12.4964");
        entity.setNormalizedAddress(normalized);

        return entity;
    }

    private RaddRegistryEntity buildRegistryEntityV1() {
        RaddRegistryEntity entity = new RaddRegistryEntity();
        entity.setRegistryId(registryId);
        entity.setCxId(partnerId);
        entity.setPhoneNumber("123456789");
        entity.setExternalCode("ext-001");
        entity.setDescription("Test description");
        entity.setOpeningTime("08:00-18:00");
        entity.setStartValidity(Instant.parse("2025-01-01T00:00:00Z"));
        entity.setEndValidity(Instant.parse("2025-12-31T23:59:59Z"));

        NormalizedAddressEntity address = new NormalizedAddressEntity();
        address.setAddressRow("Via Roma 1");
        address.setCap("00100");
        address.setCity("Roma");
        address.setPr("RM");
        address.setCountry("Italia");
        entity.setNormalizedAddress(address);

        entity.setGeoLocation("{\"latitude\":\"41.9028\", \"longitude\":\"12.4964\"}");

        return entity;
    }

    private RaddRegistryOriginalRequest buildOriginalRequest() {
        RaddRegistryOriginalRequest request = new RaddRegistryOriginalRequest();
        request.setCity("Roma");
        request.setPr("RM");
        request.setCountry("Italia");
        request.setAddressRow("Via Roma 1");
        request.setCap("00100");
        request.setPhoneNumber("123456789");
        request.setExternalCode("ext-001");
        request.setDescription("Test description");
        request.setOpeningTime("08:00-18:00");
        request.setStartValidity("2025-01-01T00:00:00Z");
        request.setEndValidity("2025-12-31T23:59:59Z");
        return request;
    }

    private AwsGeoService.CoordinatesResult buildCoordinatesResult() {
        AwsGeoService.CoordinatesResult result = new AwsGeoService.CoordinatesResult();
        result.setAwsLatitude("41.9028");
        result.setAwsLongitude("12.4964");
        result.setAwsCountry("Italia");
        result.setAwsAddressRow("Via Roma 1");
        result.setAwsPostalCode("00100");
        result.setAwsLocality("Roma");
        result.setAwsSubRegion("RM");
        return result;
    }


    @Test
    void testMappingToV1_success() {
        Registry v1 = registryMappingUtils.mappingToV1(v2);
        Instant instantStart = Instant.parse("2025-01-01T00:00:00Z");
        Date dateStart = Date.from(instantStart);

        Instant instantEnd = Instant.parse("2025-12-31T23:59:59Z");
        Date dateEnd = Date.from(instantEnd);


        assertNotNull(v1);
        assertEquals("loc-123", v1.getRegistryId());
        assertEquals("123456789", v1.getPhoneNumber());
        assertEquals("ext-001", v1.getExternalCode());
        assertEquals("Test description", v1.getDescription());
        assertEquals("08:00-18:00", v1.getOpeningTime());
        assertEquals(dateStart, v1.getStartValidity());
        assertEquals(dateEnd, v1.getEndValidity());
        assertNotNull(v1.getAddress());
        assertEquals("Via Roma 1", v1.getAddress().getAddressRow());
        assertEquals("00100", v1.getAddress().getCap());
        assertEquals("00100", v1.getAddress().getCap());
        assertEquals("Roma", v1.getAddress().getCity());
        assertEquals("RM", v1.getAddress().getPr());
        assertEquals("Italia", v1.getAddress().getCountry());
        assertEquals("00100", v1.getAddress().getCap());

        assertNull(v1.getCapacity(), "Capacity deve essere null perché in V2 non esiste");
    }

    @Test
    void testMappingToV1_withNullInput() {
        assertNull(registryMappingUtils.mappingToV1(null));
    }

    @Test
    void testMappingToV2_withNullInput() {
        assertNull(registryMappingUtils.mappingToV2(null, null, null, null, null));
    }

    @Test
    void testMappingToV2_updateWithNullInput() {
        assertNull(registryMappingUtils.mappingToV2(null, null, null, null));
    }

    @Test
    void testMappingToV1_withEmptyLists() {
        v2.setPhoneNumbers(List.of());
        v2.setExternalCodes(List.of());
        Registry v1 = registryMappingUtils.mappingToV1(v2);
        assertNull(v1.getPhoneNumber());
        assertNull(v1.getExternalCode());
    }

    @Test
    void testMappingToV1_withEmptyAddress() {
        v2.setNormalizedAddress(new NormalizedAddressEntityV2());
        v2.getNormalizedAddress().setLatitude("");
        v2.getNormalizedAddress().setLongitude("");

        Registry v1 = registryMappingUtils.mappingToV1(v2);
        assertNotNull(v1);
        assertNotNull(v1.getAddress());
        assertNull(v1.getAddress().getAddressRow());
        assertNull(v1.getAddress().getCap());
        assertNull(v1.getAddress().getCity());
        assertNull(v1.getAddress().getPr());
        assertNull(v1.getAddress().getCountry());
        assertNull(v1.getAddress().getCap());
    }

    @Test
    void testMappingToV2_success() {
        RaddRegistryEntityV2 v2Mapped = registryMappingUtils.mappingToV2( registryId, uid, coordinateResult, registryRequest, originalRequest);

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
        assertEquals("Via Roma 1", v2Mapped.getAddress().getAddressRow());
        assertEquals("00100", v2Mapped.getAddress().getCap());
        assertEquals("Roma", v2Mapped.getAddress().getCity());
        assertEquals("RM", v2Mapped.getAddress().getProvince());
        assertEquals("Italia", v2Mapped.getAddress().getCountry());
        assertEquals("41.9028", v2Mapped.getNormalizedAddress().getLatitude());
        assertEquals("12.4964", v2Mapped.getNormalizedAddress().getLongitude());
        assertEquals(Const.PARTNERTYPE_CAF, v2Mapped.getPartnerType());
    }



    @Test
    void testMappingToV2_withEmptyStrings() {
        registryId="";
        registryRequest.setCxId("");
        originalRequest.setPhoneNumber("");
        originalRequest.setExternalCode("");
        originalRequest.setDescription("");
        originalRequest.setOpeningTime("");
        RaddRegistryEntityV2 v2Mapped = registryMappingUtils.mappingToV2(registryId, uid, coordinateResult, registryRequest, originalRequest);

        assertNotNull(v2Mapped);
        assertEquals("", v2Mapped.getLocationId());
        assertEquals("", v2Mapped.getPartnerId());
        assertEquals(List.of(""), v2Mapped.getPhoneNumbers());
        assertEquals(List.of(""), v2Mapped.getExternalCodes());
        assertEquals("", v2Mapped.getDescription());
        assertEquals("", v2Mapped.getOpeningTime());
        assertEquals(uid, v2Mapped.getUid());
    }



    @Test
    void testMappingToV2_updateWithEmptyStrings() {
        uid="";
        registryRequest.setCxId("");
        originalRequest.setPhoneNumber("");
        originalRequest.setExternalCode("");
        originalRequest.setDescription("");
        originalRequest.setOpeningTime("");
        registryRequest.setZipCode("");
        v2.setLocationId("");
        v2.setPartnerId("");
        v2.setPhoneNumbers(List.of(""));
        v2.setExternalCodes(List.of(""));
        v2.setDescription("");
        v2.setOpeningTime("");

        RaddRegistryEntityV2 v2Mapped = registryMappingUtils.mappingToV2(uid, v2 , registryRequest, originalRequest);

        assertNotNull(v2Mapped);
        assertEquals("", v2Mapped.getLocationId());
        assertEquals("", v2Mapped.getPartnerId());
        assertEquals(List.of(""), v2Mapped.getPhoneNumbers());
        assertEquals(List.of(""), v2Mapped.getExternalCodes());
        assertEquals("", v2Mapped.getDescription());
        assertEquals("", v2Mapped.getOpeningTime());
        assertEquals(uid, v2Mapped.getUid());
    }

    @Test
    void testMappingToV2_updateSuccess() {
        RaddRegistryEntityV2 v2Mapped = registryMappingUtils.mappingToV2(uid, v2 , registryRequest, originalRequest);

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
        assertEquals("Via Roma 1", v2Mapped.getAddress().getAddressRow());
        assertEquals("00100", v2Mapped.getAddress().getCap());
        assertEquals("Roma", v2Mapped.getAddress().getCity());
        assertEquals("RM", v2Mapped.getAddress().getProvince());
        assertEquals("Italia", v2Mapped.getAddress().getCountry());
        assertEquals("41.9028", v2Mapped.getNormalizedAddress().getLatitude());
        assertEquals("12.4964", v2Mapped.getNormalizedAddress().getLongitude());
        assertEquals(Const.PARTNERTYPE_CAF, v2Mapped.getPartnerType());
    }

}

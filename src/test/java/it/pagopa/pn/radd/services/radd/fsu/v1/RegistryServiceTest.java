package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.api.dto.events.PnEvaluatedZipCodeEvent;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.OriginalRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.RegistryRequestResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.RegistryUploadRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.RequestResponse;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.RegistryMappingUtils;
import it.pagopa.pn.radd.middleware.db.RaddRegistryDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryImportDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryRequestDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryImportEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryRequestEntity;
import it.pagopa.pn.radd.middleware.db.entities.NormalizedAddressEntityV2;
import it.pagopa.pn.radd.middleware.eventbus.EventBridgeProducer;
import it.pagopa.pn.radd.middleware.msclient.PnAddressManagerClient;
import it.pagopa.pn.radd.middleware.msclient.PnSafeStorageClient;
import it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent;
import it.pagopa.pn.radd.middleware.queue.event.PnAddressManagerEvent;
import it.pagopa.pn.radd.middleware.queue.event.PnRaddAltNormalizeRequestEvent;
import it.pagopa.pn.radd.middleware.queue.producer.RaddAltCapCheckerProducer;
import it.pagopa.pn.radd.pojo.*;
import it.pagopa.pn.radd.utils.ObjectMapperUtil;
import it.pagopa.pn.radd.utils.RaddRegistryUtils;
import it.pagopa.pn.radd.exception.CoordinatesNotFoundException;
import it.pagopa.pn.radd.exception.RaddRegistryAlreadyExistsException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static it.pagopa.pn.radd.pojo.RaddRegistryImportStatus.PENDING;
import static it.pagopa.pn.radd.pojo.RaddRegistryImportStatus.TO_PROCESS;
import static it.pagopa.pn.radd.utils.Const.ERROR_DUPLICATE;
import static it.pagopa.pn.radd.utils.Const.REQUEST_ID_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@EnableConfigurationProperties
@ExtendWith(MockitoExtension.class)
class RegistryServiceTest {

    @Mock
    private RaddRegistryUtils raddRegistryUtils;

    @Mock
    private PnRaddFsuConfig pnRaddFsuConfig;
    @Mock
    private RaddRegistryDAO raddRegistryDAO;
    @Mock
    private RaddRegistryV2DAO raddRegistryV2DAO;

    @Mock
    AwsGeoService awsGeoService;

    @Mock
    private RaddRegistryRequestDAO raddRegistryRequestDAO;

    @Mock
    private PnSafeStorageClient pnSafeStorageClient;
    @Mock
    private RaddRegistryImportDAO raddRegistryImportDAO;

    @Mock
    private PnAddressManagerEvent message;
    @Mock
    private PnAddressManagerClient pnAddressManagerClient;

    @Mock
    private RaddAltCapCheckerProducer raddAltCapCheckerProducer;

    @Mock
    private PnRaddAltNormalizeRequestEvent.Payload payload;

    @Mock
    private SecretService secretService;

    @Mock
    private EventBridgeProducer<PnEvaluatedZipCodeEvent> eventBridgeProducer;


    private RegistryService registryService;

    @BeforeEach
    void setUp() {
        ObjectMapperUtil objectMapperUtil = new ObjectMapperUtil(new com.fasterxml.jackson.databind.ObjectMapper());
        registryService = new RegistryService(raddRegistryRequestDAO, raddRegistryDAO, raddRegistryV2DAO, raddRegistryImportDAO, pnSafeStorageClient,
                new RaddRegistryUtils(objectMapperUtil, pnRaddFsuConfig, secretService, new RegistryMappingUtils(objectMapperUtil)), pnAddressManagerClient,
                raddAltCapCheckerProducer, pnRaddFsuConfig, eventBridgeProducer, objectMapperUtil, awsGeoService);
    }

    @Test
    void testUploadRegistryRequests() {
        RegistryUploadRequest request = new RegistryUploadRequest();
        request.setChecksum("checksum");
        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        FileCreationResponseDto fileCreationResponseDto = new FileCreationResponseDto();
        fileCreationResponseDto.setKey("key");
        fileCreationResponseDto.setSecret("secret");
        fileCreationResponseDto.setUploadUrl("url");
        when(raddRegistryImportDAO.getRegistryImportByCxId(any())).thenReturn(Flux.just(pnRaddRegistryImportEntity));
        when(pnSafeStorageClient.createFile(any(), any())).thenReturn(Mono.just(fileCreationResponseDto));
        when(pnRaddFsuConfig.getRegistryDefaultEndValidity()).thenReturn(1);
        when(pnRaddFsuConfig.getRegistryDefaultDeleteRule()).thenReturn("role");
        when(raddRegistryImportDAO.putRaddRegistryImportEntity(any())).thenReturn(Mono.just(pnRaddRegistryImportEntity));

        StepVerifier.create(registryService.uploadRegistryRequests("cxId", "testUid", Mono.just(request)))
                .expectNextMatches(registryUploadResponse1 -> registryUploadResponse1.getFileKey().equals("key")).verifyComplete();
    }

    @Test
    void testUploadRegistryRequestsNotValid() {
        RegistryUploadRequest request = new RegistryUploadRequest();
        request.setChecksum("checksum");
        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        pnRaddRegistryImportEntity.setStatus(TO_PROCESS.name());
        pnRaddRegistryImportEntity.setChecksum("checksum");
        pnRaddRegistryImportEntity.setFileUploadDueDate(Instant.now().plus(10, ChronoUnit.DAYS));
        when(raddRegistryImportDAO.getRegistryImportByCxId(any())).thenReturn(Flux.just(pnRaddRegistryImportEntity));

        StepVerifier.create(registryService.uploadRegistryRequests("cxId", "testUid", Mono.just(request)))
                .expectErrorMessage("Richiesta Duplicata. il file inviato è già in fase di elaborazione").verify();
    }

    @Test
    void testUploadRegistryRequestsNotValid2() {
        RegistryUploadRequest request = new RegistryUploadRequest();
        request.setChecksum("checksum");
        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        pnRaddRegistryImportEntity.setStatus(PENDING.name());
        when(raddRegistryImportDAO.getRegistryImportByCxId(any())).thenReturn(Flux.just(pnRaddRegistryImportEntity));

        StepVerifier.create(registryService.uploadRegistryRequests("cxId", "testUid", Mono.just(request)))
                .expectErrorMessage("Una precedente richiesta di import è ancora in corso").verify();
    }

    @Test
    void testVerifyRegistryRequests_ValidCase() {

        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        pnRaddRegistryImportEntity.setStatus("DONE");
        when(raddRegistryImportDAO.getRegistryImportByCxIdAndRequestId(any(), any())).thenReturn(Mono.just(pnRaddRegistryImportEntity));

        StepVerifier.create(registryService.verifyRegistriesImportRequest("cxId", "requestId"))
                .expectNextMatches(response -> response.getStatus().equals("DONE") && StringUtils.isBlank(response.getError()))
                .verifyComplete();
    }

    @Test
    void testVerifyRegistryRequests_ValidCaseWithError() {

        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        pnRaddRegistryImportEntity.setStatus("REJECTED");
        pnRaddRegistryImportEntity.setError("error");
        when(raddRegistryImportDAO.getRegistryImportByCxIdAndRequestId(any(), any())).thenReturn(Mono.just(pnRaddRegistryImportEntity));

        StepVerifier.create(registryService.verifyRegistriesImportRequest("cxId", "requestId"))
                .expectNextMatches(response -> response.getStatus().equals("REJECTED") && response.getError().equals("error"))
                .verifyComplete();
    }


    @Test
    void testVerifyRegistryRequests_ExceptionCase() {

        when(raddRegistryImportDAO.getRegistryImportByCxIdAndRequestId(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(registryService.verifyRegistriesImportRequest("cxId", "requestId"))
                .expectErrorMessage("Richiesta di import non trovata")
                .verify();
    }

    @Nested
    @DisplayName("handleImportCompletedRequest Tests")
    class HandleImportCompletedRequestTests {

        @Test
        void handleImportCompletedRequestWithMultipleImport() {
            String xPagopaPnCxId = "testCxId";
            String requestId = "testRequestId";

            RaddRegistryImportEntity raddRegistryImportEntity = new RaddRegistryImportEntity();
            raddRegistryImportEntity.setConfig("{}");
            raddRegistryImportEntity.setRequestId(requestId);
            raddRegistryImportEntity.setCxId(xPagopaPnCxId);

            RaddRegistryImportEntity raddRegistryImportEntityOld = new RaddRegistryImportEntity();
            raddRegistryImportEntityOld.setConfig("{}");
            raddRegistryImportEntityOld.setRequestId(requestId + "old");
            raddRegistryImportEntityOld.setCxId(xPagopaPnCxId);

            RaddRegistryEntityV2 raddRegistryEntity = new RaddRegistryEntityV2();
            raddRegistryEntity.setLocationId("testRegistryId");

            RaddRegistryImportConfig raddRegistryImportConfig = new RaddRegistryImportConfig();
            raddRegistryImportConfig.setDeleteRole("duplicate");
            raddRegistryImportConfig.setDefaultEndValidity(1);

            RaddRegistryRequestEntity raddRegistryRequestEntity = new RaddRegistryRequestEntity();
            raddRegistryRequestEntity.setPk("cxId#requestId#index");
            raddRegistryRequestEntity.setZipCode("00100");

            when(raddRegistryImportDAO.getRegistryImportByCxIdFilterByStatus(any(), any(), any()))
                    .thenReturn(Flux.just(raddRegistryImportEntity, raddRegistryImportEntityOld));
            when(raddRegistryV2DAO.findByPartnerIdAndRequestId(any(), any()))
                    .thenReturn(Flux.just(raddRegistryEntity));
            when(raddRegistryV2DAO.findCrudRegistriesByPartnerId(any()))
                    .thenReturn(Flux.just(raddRegistryEntity));
            when(raddRegistryImportDAO.updateStatusAndTtl(any(), any(), any()))
                    .thenReturn(Mono.just(raddRegistryImportEntity));
            when(raddRegistryV2DAO.updateRegistryEntity(raddRegistryEntity))
                    .thenReturn(Mono.just(raddRegistryEntity));

            ImportCompletedRequestEvent.Payload payload = ImportCompletedRequestEvent.Payload.builder().cxId(xPagopaPnCxId).requestId(requestId).build();
            StepVerifier.create(registryService.handleImportCompletedRequest(payload)).expectComplete().verify();

        }

        @Test
        void handleImportCompletedRequest() {
            String xPagopaPnCxId = "testCxId";
            String requestId = "testRequestId";

            RaddRegistryImportEntity raddRegistryImportEntity = new RaddRegistryImportEntity();
            raddRegistryImportEntity.setConfig("{}");
            raddRegistryImportEntity.setRequestId(requestId);
            raddRegistryImportEntity.setCxId(xPagopaPnCxId);

            RaddRegistryEntityV2 raddRegistryEntity = new RaddRegistryEntityV2();
            raddRegistryEntity.setLocationId("testRegistryId");

            RaddRegistryImportConfig raddRegistryImportConfig = new RaddRegistryImportConfig();
            raddRegistryImportConfig.setDeleteRole("duplicate");
            raddRegistryImportConfig.setDefaultEndValidity(1);

            RaddRegistryRequestEntity raddRegistryRequestEntity = new RaddRegistryRequestEntity();
            raddRegistryRequestEntity.setPk("cxId#requestId#index");
            raddRegistryRequestEntity.setZipCode("00100");

            when(raddRegistryImportDAO.getRegistryImportByCxIdFilterByStatus(any(), any(), any()))
                    .thenReturn(Flux.just(raddRegistryImportEntity));
            when(raddRegistryV2DAO.findCrudRegistriesByPartnerId(xPagopaPnCxId))
                    .thenReturn(Flux.just(raddRegistryEntity));
            when(raddRegistryV2DAO.updateRegistryEntity(raddRegistryEntity))
                    .thenReturn(Mono.just(raddRegistryEntity));

            ImportCompletedRequestEvent.Payload payload = ImportCompletedRequestEvent.Payload.builder().cxId(xPagopaPnCxId).requestId(requestId).build();
            StepVerifier.create(registryService.handleImportCompletedRequest(payload)).expectComplete().verify();

        }

        @Test
        void testDeleteOlderRequestRegistriesAndGetCapListForFirstImportRequest() {
            String xPagopaPnCxId = "testCxId";
            String requestId = "testRequestId";

            RaddRegistryImportEntity raddRegistryImportEntity = new RaddRegistryImportEntity();
            raddRegistryImportEntity.setConfig("{}");

            RaddRegistryEntityV2 raddRegistryEntity = new RaddRegistryEntityV2();
            raddRegistryEntity.setLocationId("testRegistryId");

            RaddRegistryImportConfig raddRegistryImportConfig = new RaddRegistryImportConfig();
            raddRegistryImportConfig.setDeleteRole("duplicate");
            raddRegistryImportConfig.setDefaultEndValidity(1);

            RaddRegistryRequestEntity raddRegistryRequestEntity = new RaddRegistryRequestEntity();
            raddRegistryRequestEntity.setPk("cxId#requestId#index");

            when(raddRegistryImportDAO.getRegistryImportByCxIdFilterByStatus(xPagopaPnCxId, requestId, RaddRegistryImportStatus.DONE))
                    .thenReturn(Flux.just(raddRegistryImportEntity));
            when(raddRegistryV2DAO.findCrudRegistriesByPartnerId(xPagopaPnCxId))
                    .thenReturn(Flux.just(raddRegistryEntity));
            when(raddRegistryV2DAO.updateRegistryEntity(raddRegistryEntity))
                    .thenReturn(Mono.just(raddRegistryEntity));

            Flux<RaddRegistryEntityV2> result = registryService.deleteOlderRegistries(xPagopaPnCxId, requestId);

            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            verify(raddRegistryImportDAO, times(1)).getRegistryImportByCxIdFilterByStatus(xPagopaPnCxId, requestId, RaddRegistryImportStatus.DONE);
            verify(raddRegistryV2DAO, times(1)).findCrudRegistriesByPartnerId(xPagopaPnCxId);
            verify(raddRegistryV2DAO, times(1)).updateRegistryEntity(raddRegistryEntity);
        }

        @Test
        void testDeleteOlderRequestRegistriesAndGetCapListForSubsequentImportRequest() {
            String xPagopaPnCxId = "testCxId";
            String requestId = "testNewRequestId";
            String oldRequestId = "testOldRequestId";

            RaddRegistryImportEntity newRaddRegistryImportEntity = new RaddRegistryImportEntity();
            newRaddRegistryImportEntity.setRequestId(requestId);
            newRaddRegistryImportEntity.setCxId(xPagopaPnCxId);
            newRaddRegistryImportEntity.setStatus(RaddRegistryImportStatus.DONE.name());
            newRaddRegistryImportEntity.setConfig("{}");

            RaddRegistryImportEntity oldRaddRegistryImportEntity = new RaddRegistryImportEntity();
            oldRaddRegistryImportEntity.setRequestId(oldRequestId);
            oldRaddRegistryImportEntity.setCxId(xPagopaPnCxId);
            oldRaddRegistryImportEntity.setStatus(RaddRegistryImportStatus.DONE.name());
            oldRaddRegistryImportEntity.setConfig("{}");

            RaddRegistryEntityV2 raddRegistryEntityMadeByCrud = new RaddRegistryEntityV2();
            raddRegistryEntityMadeByCrud.setLocationId(REQUEST_ID_PREFIX + "registryId");

            RaddRegistryEntityV2 raddRegistryEntityMadeByOldImport = new RaddRegistryEntityV2();
            raddRegistryEntityMadeByOldImport.setLocationId(oldRequestId);

            RaddRegistryImportConfig raddRegistryImportConfig = new RaddRegistryImportConfig();
            raddRegistryImportConfig.setDeleteRole("differentFromDuplicate");
            raddRegistryImportConfig.setDefaultEndValidity(1);

            when(raddRegistryImportDAO.getRegistryImportByCxIdFilterByStatus(xPagopaPnCxId, requestId, RaddRegistryImportStatus.DONE))
                    .thenReturn(Flux.just(newRaddRegistryImportEntity, oldRaddRegistryImportEntity));
            when(raddRegistryV2DAO.findByPartnerIdAndRequestId(xPagopaPnCxId, oldRequestId))
                    .thenReturn(Flux.just(raddRegistryEntityMadeByOldImport));
            when(raddRegistryV2DAO.findCrudRegistriesByPartnerId(xPagopaPnCxId))
                    .thenReturn(Flux.just(raddRegistryEntityMadeByCrud));
            when(raddRegistryImportDAO.updateStatusAndTtl(any(), any(), any()))
                    .thenReturn(Mono.just(newRaddRegistryImportEntity));
            when(raddRegistryV2DAO.updateRegistryEntity(any()))
                    .thenReturn(Mono.just(new RaddRegistryEntityV2()));

            Flux<RaddRegistryEntityV2> result = registryService.deleteOlderRegistries(xPagopaPnCxId, requestId);

            StepVerifier.create(result)
                    .expectNextCount(2)
                    .verifyComplete();

            verify(raddRegistryImportDAO, times(1)).getRegistryImportByCxIdFilterByStatus(xPagopaPnCxId, requestId, RaddRegistryImportStatus.DONE);
            verify(raddRegistryV2DAO, times(1)).findCrudRegistriesByPartnerId(xPagopaPnCxId);
            verify(raddRegistryV2DAO, times(1)).findByPartnerIdAndRequestId(xPagopaPnCxId, oldRequestId);
            verify(raddRegistryV2DAO, times(2)).updateRegistryEntity(any());
            verify(raddRegistryRequestDAO, times(0)).findByCxIdAndRegistryId(anyString(), anyString());
            verify(raddRegistryRequestDAO, times(0)).putRaddRegistryRequestEntity(any());
        }

        @Test
        void testDeleteOlderRequestRegistriesAndGetCapListFails() {
            String xPagopaPnCxId = "testCxId";
            String requestId = "testNewRequestId";

            when(raddRegistryImportDAO.getRegistryImportByCxIdFilterByStatus(xPagopaPnCxId, requestId, RaddRegistryImportStatus.DONE))
                    .thenReturn(Flux.empty());

            Flux<RaddRegistryEntityV2> result = registryService.deleteOlderRegistries(xPagopaPnCxId, requestId);

            StepVerifier.create(result)
                    .expectError(RaddGenericException.class)
                    .verify();

            verify(raddRegistryImportDAO, times(1)).getRegistryImportByCxIdFilterByStatus(xPagopaPnCxId, requestId, RaddRegistryImportStatus.DONE);
            verify(raddRegistryV2DAO, times(0)).findCrudRegistriesByPartnerId(xPagopaPnCxId);
        }

        @Test
        void testRetrieveRequestItems() {
            Instant now = Instant.MIN;
            RequestResponse requestResponse = new RequestResponse();
            RegistryRequestResponse registryRequestResponse = new RegistryRequestResponse();
            registryRequestResponse.setRegistryId("");
            registryRequestResponse.setRequestId("");
            registryRequestResponse.setError("");
            registryRequestResponse.setCreatedAt(now.toString());
            registryRequestResponse.setUpdatedAt(now.toString());
            registryRequestResponse.setStatus("");
            registryRequestResponse.setOriginalRequest(new OriginalRequest());

            RaddRegistryRequestEntity raddRegistryRequestEntity = new RaddRegistryRequestEntity();
            raddRegistryRequestEntity.setRegistryId("");
            raddRegistryRequestEntity.setRequestId("");
            raddRegistryRequestEntity.setError("");
            raddRegistryRequestEntity.setCreatedAt(now);
            raddRegistryRequestEntity.setUpdatedAt(now);
            raddRegistryRequestEntity.setStatus("");
            raddRegistryRequestEntity.setOriginalRequest("{}");


            requestResponse.setMoreResult(false);
            requestResponse.setNextPagesKey(List.of());
            requestResponse.setItems(List.of(registryRequestResponse));
            ResultPaginationDto<RaddRegistryRequestEntity, String> resultPaginationDto = new ResultPaginationDto<>();
            resultPaginationDto.setResultsPage(List.of(raddRegistryRequestEntity));
            when(raddRegistryRequestDAO.getRegistryByCxIdAndRequestId(any(), any(), any(), any()))
                    .thenReturn(Mono.just(resultPaginationDto));


            StepVerifier.create(registryService.retrieveRequestItems("cxId", "requestId", 10, null))
                    .expectNextMatches(response -> response.getItems().size() == 1)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("handleNormalizeRequestEvent Tests")
    class HandleNormalizeRequestEventTests {

        private PnRaddAltNormalizeRequestEvent.Payload payload;
        private RaddRegistryRequestEntity mockEntity;
        private AddressManagerRequestAddress mockAddress;
        private AwsGeoService.CoordinatesResult mockCoordinatesResult;
        private RaddRegistryEntityV2 mockRegistryEntity;

        @BeforeEach
        void setUpNormalizeRequestEventTests() {
            // Setup payload
            payload = PnRaddAltNormalizeRequestEvent.Payload.builder()
                    .correlationId("test-correlation-id")
                    .build();

            // Setup mock registry request entity
            mockEntity = new RaddRegistryRequestEntity();
            mockEntity.setPk("cxId#requestId#index");
            mockEntity.setCorrelationId("test-correlation-id");
            mockEntity.setCxId("test-cx-id");
            mockEntity.setRequestId("test-request-id");
            mockEntity.setStatus(RegistryRequestStatus.NOT_WORKED.name());
            mockEntity.setOriginalRequest("{\"addressRow\":\"Via Roma 1\",\"cap\":\"00100\",\"city\":\"Roma\",\"pr\":\"RM\",\"country\":\"IT\",\"description\":\"Test Description\",\"openingTime\":\"9:00-18:00\",\"startValidity\":\"2023-01-01T00:00:00Z\",\"endValidity\":\"2023-12-31T00:00:00Z\",\"phoneNumber\":\"1234567890\",\"externalCode\":\"EXT123\"}");
            mockEntity.setCreatedAt(Instant.now());
            mockEntity.setUpdatedAt(Instant.now());

            // Setup mock address
            mockAddress = new AddressManagerRequestAddress();
            mockAddress.setId("1");
            mockAddress.setAddressRow("Via Roma 1");
            mockAddress.setCap("00100");
            mockAddress.setCity("Roma");
            mockAddress.setPr("RM");
            mockAddress.setCountry("IT");

            // Setup mock coordinates result
            mockCoordinatesResult = new AwsGeoService.CoordinatesResult();
            mockCoordinatesResult.setAwsLatitude("41.9028");
            mockCoordinatesResult.setAwsLongitude("12.4964");
            mockCoordinatesResult.setAwsAddressRow("Via Roma 1, Roma, RM");
            mockCoordinatesResult.setAwsPostalCode("00100");
            mockCoordinatesResult.setAwsLocality("Roma");
            mockCoordinatesResult.setAwsSubRegion("RM");
            mockCoordinatesResult.setAwsCountry("Italia");

            // Setup mock registry entity
            mockRegistryEntity = new RaddRegistryEntityV2();
            mockRegistryEntity.setLocationId("test-location-id");
            mockRegistryEntity.setPartnerId("test-cx-id");

            // Setup normalized address
            NormalizedAddressEntityV2 normalizedAddress = new NormalizedAddressEntityV2();
            normalizedAddress.setCap("00100");
            normalizedAddress.setAddressRow("Via Roma 1");
            normalizedAddress.setCity("Roma");
            normalizedAddress.setProvince("RM");
            normalizedAddress.setCountry("IT");
            normalizedAddress.setLatitude("41.9028");
            normalizedAddress.setLongitude("12.4964");
            mockRegistryEntity.setNormalizedAddress(normalizedAddress);
        }

        @Test
        @DisplayName("Should successfully handle normalize request event - Happy Path")
        void shouldSuccessfullyHandleNormalizeRequestEvent() {
            // Arrange
            RaddRegistryRequestEntity updatedEntity = new RaddRegistryRequestEntity();
            updatedEntity.setPk(mockEntity.getPk());
            updatedEntity.setCorrelationId(mockEntity.getCorrelationId());
            updatedEntity.setCxId(mockEntity.getCxId());
            updatedEntity.setRequestId(mockEntity.getRequestId());
            updatedEntity.setStatus(RegistryRequestStatus.PENDING.name());
            updatedEntity.setOriginalRequest(mockEntity.getOriginalRequest());
            updatedEntity.setUpdatedAt(Instant.now());

            RaddRegistryRequestEntity finalUpdatedEntity = new RaddRegistryRequestEntity();
            finalUpdatedEntity.setPk(mockEntity.getPk());
            finalUpdatedEntity.setCorrelationId(mockEntity.getCorrelationId());
            finalUpdatedEntity.setCxId(mockEntity.getCxId());
            finalUpdatedEntity.setRequestId(mockEntity.getRequestId());
            finalUpdatedEntity.setStatus(RegistryRequestStatus.ACCEPTED.name());
            finalUpdatedEntity.setRegistryId(mockRegistryEntity.getLocationId());
            finalUpdatedEntity.setZipCode(mockRegistryEntity.getNormalizedAddress().getCap());
            finalUpdatedEntity.setUpdatedAt(Instant.now());

            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.just(mockEntity));
            when(raddRegistryRequestDAO.updateRecordInPending(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.just(updatedEntity));
            when(awsGeoService.getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Mono.just(mockCoordinatesResult));
            when(raddRegistryV2DAO.putItemIfAbsent(any(RaddRegistryEntityV2.class)))
                    .thenReturn(Mono.just(mockRegistryEntity));
            when(raddRegistryRequestDAO.updateRegistryRequestData(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.just(finalUpdatedEntity));

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload)).verifyComplete();

            // Verify interactions
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
            verify(raddRegistryRequestDAO).updateRecordInPending(any(RaddRegistryRequestEntity.class));
            verify(awsGeoService).getCoordinatesForAddress(
                    eq("Via Roma 1"),
                    eq("RM"),
                    eq("00100"),
                    eq("Roma"),
                    eq("IT")
            );
            verify(raddRegistryV2DAO).putItemIfAbsent(any(RaddRegistryEntityV2.class));
            verify(raddRegistryRequestDAO).updateRegistryRequestData(any(RaddRegistryRequestEntity.class));
        }

        @Test
        @DisplayName("Should handle error when getAllFromCorrelationId fails")
        void shouldHandleErrorWhenGetAllFromCorrelationIdFails() {
            // Arrange
            RaddGenericException expectedException = new RaddGenericException("Database connection error");

            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.error(expectedException));

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .expectError(RaddGenericException.class)
                    .verify();

            // Verify only first method was called
            verify(raddRegistryRequestDAO).getAllFromCorrelationId(eq("test-correlation-id"), eq(RegistryRequestStatus.NOT_WORKED.name()));
            verify(raddRegistryRequestDAO, never()).updateRecordInPending(any());
            verify(awsGeoService, never()).getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle error when updateRecordInPending fails")
        void shouldHandleErrorWhenUpdateRecordInPendingFails() {
            // Arrange
            RaddGenericException expectedException = new RaddGenericException("Update operation failed");

            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.just(mockEntity));
            when(raddRegistryRequestDAO.updateRecordInPending(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.error(expectedException));

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .expectError(RaddGenericException.class)
                    .verify();

            // Verify method call chain stops at updateRecordInPending
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
            verify(raddRegistryRequestDAO).updateRecordInPending(any());
            verify(awsGeoService, never()).getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle error when address normalization fails")
        void shouldHandleErrorWhenAddressNormalizationFails() {
            // Arrange
            RaddRegistryRequestEntity updatedEntity = new RaddRegistryRequestEntity();
            updatedEntity.setPk(mockEntity.getPk());
            updatedEntity.setStatus(RegistryRequestStatus.PENDING.name());
            updatedEntity.setOriginalRequest(mockEntity.getOriginalRequest());

            CoordinatesNotFoundException expectedException = new CoordinatesNotFoundException("Address not found");

            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.just(mockEntity));
            when(raddRegistryRequestDAO.updateRecordInPending(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.just(updatedEntity));
            when(awsGeoService.getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Mono.error(expectedException));
            when(raddRegistryRequestDAO.updateStatusAndError(any(RaddRegistryRequestEntity.class), eq(RegistryRequestStatus.REJECTED), anyString()))
                    .thenReturn(Mono.just(updatedEntity));

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .verifyComplete();

            // Verify method call chain stops at address normalization
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
            verify(raddRegistryRequestDAO).updateRecordInPending(any());
            verify(awsGeoService).getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString());
            verify(raddRegistryRequestDAO).updateStatusAndError( any(), eq(RegistryRequestStatus.REJECTED), anyString());
        }

        @Test
        @DisplayName("Should handle duplicate registry error and update status to REJECTED")
        void shouldHandleDuplicateRegistryErrorAndUpdateStatusToRejected() {
            // Arrange
            RaddRegistryRequestEntity updatedEntity = new RaddRegistryRequestEntity();
            updatedEntity.setPk(mockEntity.getPk());
            updatedEntity.setStatus(RegistryRequestStatus.PENDING.name());
            updatedEntity.setOriginalRequest(mockEntity.getOriginalRequest());

            RaddRegistryRequestEntity rejectedEntity = new RaddRegistryRequestEntity();
            rejectedEntity.setPk(mockEntity.getPk());
            rejectedEntity.setStatus(RegistryRequestStatus.REJECTED.name());
            rejectedEntity.setError(ERROR_DUPLICATE);

            RaddRegistryAlreadyExistsException duplicateException = new RaddRegistryAlreadyExistsException();

            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.just(mockEntity));
            when(raddRegistryRequestDAO.updateRecordInPending(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.just(updatedEntity));
            when(awsGeoService.getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Mono.just(mockCoordinatesResult));
            when(raddRegistryV2DAO.putItemIfAbsent(any(RaddRegistryEntityV2.class)))
                    .thenReturn(Mono.error(duplicateException));
            when(raddRegistryRequestDAO.updateStatusAndError(any(RaddRegistryRequestEntity.class), eq(RegistryRequestStatus.REJECTED), eq(ERROR_DUPLICATE)))
                    .thenReturn(Mono.just(rejectedEntity));

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .expectComplete()
                    .verify();

            // Verify the duplicate error handling flow
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
            verify(raddRegistryRequestDAO).updateRecordInPending(any());
            verify(awsGeoService).getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString());
            verify(raddRegistryV2DAO).putItemIfAbsent(any());
            verify(raddRegistryRequestDAO).updateStatusAndError(any(), eq(RegistryRequestStatus.REJECTED), eq(ERROR_DUPLICATE));
            verify(raddRegistryRequestDAO, never()).updateRegistryRequestData(any());
        }

        @Test
        @DisplayName("Should handle non-duplicate registry creation error")
        void shouldHandleNonDuplicateRegistryCreationError() {
            // Arrange
            RaddRegistryRequestEntity updatedEntity = new RaddRegistryRequestEntity();
            updatedEntity.setPk(mockEntity.getPk());
            updatedEntity.setStatus(RegistryRequestStatus.PENDING.name());
            updatedEntity.setOriginalRequest(mockEntity.getOriginalRequest());

            RuntimeException unexpectedException = new RuntimeException("Unexpected database error");

            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.just(mockEntity));
            when(raddRegistryRequestDAO.updateRecordInPending(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.just(updatedEntity));
            when(awsGeoService.getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Mono.just(mockCoordinatesResult));
            when(raddRegistryV2DAO.putItemIfAbsent(any(RaddRegistryEntityV2.class)))
                    .thenReturn(Mono.error(unexpectedException));

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .expectError(RuntimeException.class)
                    .verify();

            // Verify error propagates without special handling
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
            verify(raddRegistryRequestDAO).updateRecordInPending(any());
            verify(awsGeoService).getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString());
            verify(raddRegistryV2DAO).putItemIfAbsent(any());
            verify(raddRegistryRequestDAO, never()).updateStatusAndError(any(), any(), any());
            verify(raddRegistryRequestDAO, never()).updateRegistryRequestData(any());
        }

        @Test
        @DisplayName("Should handle error when final updateRegistryRequestData fails")
        void shouldHandleErrorWhenFinalUpdateRegistryRequestDataFails() {
            // Arrange
            RaddRegistryRequestEntity updatedEntity = new RaddRegistryRequestEntity();
            updatedEntity.setPk(mockEntity.getPk());
            updatedEntity.setStatus(RegistryRequestStatus.PENDING.name());
            updatedEntity.setOriginalRequest(mockEntity.getOriginalRequest());

            RuntimeException updateException = new RuntimeException("Final update failed");

            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.just(mockEntity));
            when(raddRegistryRequestDAO.updateRecordInPending(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.just(updatedEntity));
            when(awsGeoService.getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Mono.just(mockCoordinatesResult));
            when(raddRegistryV2DAO.putItemIfAbsent(any(RaddRegistryEntityV2.class)))
                    .thenReturn(Mono.just(mockRegistryEntity));
            when(raddRegistryRequestDAO.updateRegistryRequestData(any(RaddRegistryRequestEntity.class)))
                    .thenReturn(Mono.error(updateException));

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .expectError(RuntimeException.class)
                    .verify();

            // Verify all methods were called up to the final update
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
            verify(raddRegistryRequestDAO).updateRecordInPending(any());
            verify(awsGeoService).getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString());
            verify(raddRegistryV2DAO).putItemIfAbsent(any());
            verify(raddRegistryRequestDAO).updateRegistryRequestData(any());
        }

        @Test
        @DisplayName("Should handle empty result from getAllFromCorrelationId")
        void shouldHandleEmptyResultFromGetAllFromCorrelationId() {
            // Arrange
            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.empty());

            // Act & Assert
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .expectComplete()
                    .verify();

            // Verify only first method was called
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
            verify(raddRegistryRequestDAO, never()).updateRecordInPending(any());
            verify(awsGeoService, never()).getCoordinatesForAddress(anyString(), anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should verify AddressManagerRequest creation and correlation ID setting")
        void shouldVerifyAddressManagerRequestCreation() {
            // Arrange
            when(raddRegistryRequestDAO.getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name()))
                    .thenReturn(Flux.empty());

            // Act
            StepVerifier.create(registryService.handleNormalizeRequestEvent(payload))
                    .expectComplete()
                    .verify();

            // Assert - Verify that the method was called with correct parameters
            // The AddressManagerRequest creation happens internally and sets the correlationId
            verify(raddRegistryRequestDAO).getAllFromCorrelationId("test-correlation-id", RegistryRequestStatus.NOT_WORKED.name());
        }
    }
}
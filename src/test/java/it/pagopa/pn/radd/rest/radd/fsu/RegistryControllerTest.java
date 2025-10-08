package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {RegistryController.class})
@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = {RegistryController.class})
class RegistryControllerTest {
    @Autowired
    private RegistryController registryController;


    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private RegistryService registryService;
    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_UID = "x-pagopa-pn-uid";
    public static final String UID = "uid";
    private static final String BASE_PATH = "/radd-net";
    private static final String BASE_PATH_BO = "/radd-bo";



    /**
     * Method under test: {@link RegistryController#uploadRegistryRequests(CxTypeAuthFleet, String, String, Mono, ServerWebExchange)}
     */
    @Test
    void testUploadRegistryRequests3() {
        when(registryService.uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any()))
                .thenReturn(mock(Mono.class));
        registryController.uploadRegistryRequests(CxTypeAuthFleet.PA, "42", "1234", null, null);
        verify(registryService).uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any());
    }

    /**
     * Method under test: {@link RegistryController#uploadRegistryRequests(CxTypeAuthFleet, String, String, Mono, ServerWebExchange)}
     */
    @Test
    void testUploadRegistryRequests4() {
        when(registryService.uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any()))
                .thenReturn(mock(Mono.class));
        registryController.uploadRegistryRequests(CxTypeAuthFleet.PF, "42", "1234", null, null);
        verify(registryService).uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any());
    }

    /**
     * Method under test: {@link RegistryController#uploadRegistryRequests(CxTypeAuthFleet, String, String, Mono, ServerWebExchange)}
     */
    @Test
    void testUploadRegistryRequests5() {
        when(registryService.uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any()))
                .thenReturn(mock(Mono.class));
        registryController.uploadRegistryRequests(CxTypeAuthFleet.PG, "42", "1234", null, null);
        verify(registryService).uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any());
    }

    /**
     * Method under test: {@link RegistryController#uploadRegistryRequests(CxTypeAuthFleet, String, String, Mono, ServerWebExchange)}
     */
    @Test
    void testUploadRegistryRequests6() {
        when(registryService.uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any()))
                .thenReturn(mock(Mono.class));
        registryController.uploadRegistryRequests(CxTypeAuthFleet.RADD, "42", "1234", null, null);
        verify(registryService).uploadRegistryRequests(Mockito.<String>any(), anyString(), Mockito.<Mono<RegistryUploadRequest>>any());
    }

    @ParameterizedTest
    @ValueSource(strings = {BASE_PATH, BASE_PATH_BO})
    void documentUploadTest(String basePath) {
        RegistryUploadResponse response = new RegistryUploadResponse();
        RegistryUploadRequest req = new RegistryUploadRequest();

        String path = basePath + "/api/v1/registry/import/upload";
        Mockito.when(registryService.uploadRegistryRequests(Mockito.any(), anyString(), Mockito.any()))
                .thenReturn(Mono.just(response));
        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, UUID.randomUUID().toString())
                .header(UID, "myUid")
                .header( PN_PAGOPA_CX_ID, "cxId")
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .body(Mono.just(req), RegistryUploadRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {BASE_PATH, BASE_PATH_BO})
    void testVerifyRequest(String basePath) {
        // Arrange
        String xPagopaPnCxId = "cxId";
        String requestId = "requestId";
        VerifyRequestResponse expectedResponse = new VerifyRequestResponse();
        when(registryService.verifyRegistriesImportRequest(xPagopaPnCxId, requestId))
                .thenReturn(Mono.just(expectedResponse));

        String path = basePath + "/api/v1/registry/import/" + requestId + "/verify";
        // Act
        webTestClient.get()
                .uri(path)
                .header(PN_PAGOPA_UID, UUID.randomUUID().toString())
                .header(UID, "myUid")
                .header( PN_PAGOPA_CX_ID, xPagopaPnCxId)
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testRetrieveRequestItems() {
        // Arrange
        String xPagopaPnCxId = "cxId";
        String requestId = "requestId";
        RequestResponse expectedResponse = new RequestResponse();
        when(registryService.retrieveRequestItems(any(), any(), any(), any()))
                .thenReturn(Mono.just(expectedResponse));

        String path = BASE_PATH + "/api/v1/registry/import/" + requestId + "?limit=10&lastKey=lastKey";
        // Act
        webTestClient
                .get()
                .uri(path)
                .header(PN_PAGOPA_UID, UUID.randomUUID().toString())
                .header(UID, "myUid")
                .header( PN_PAGOPA_CX_ID, xPagopaPnCxId)
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }
}


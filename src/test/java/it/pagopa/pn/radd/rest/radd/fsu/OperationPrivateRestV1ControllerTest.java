package it.pagopa.pn.radd.rest.radd.fsu;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.services.radd.fsu.v1.OperationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;



@WebFluxTest(controllers = {OperationPrivateRestV1Controller.class})
class OperationPrivateRestV1ControllerTest {

    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_UID = "uid";

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private OperationService operationService;

    @Test
    void testWhenCalledActTransactionByOperationId() {
        OperationActResponse response = new OperationActResponse();
        response.setElement(new OperationActDetailResponse());

        String path = "/radd-net-private/api/v1/act/operations/by-id/{transactionId}"
                .replace("{transactionId}", "1200");
        Mockito.when(operationService
                        .getTransactionActByTransactionIdAndType(Mockito.anyString()))
                .thenReturn(Mono.just(response));
        webTestClient.get()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header( PN_PAGOPA_CX_ID, "cxId")
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testWhenCalledActTransactionByIun() {
        OperationsResponse response = new OperationsResponse();
        response.setResult(true);
        response.setOperationIds(List.of("OperationId1"));

        String path = "/radd-net-private/api/v1/act/operations/by-iun/{iun}"
                .replace("{iun}", "pppwww233");
        Mockito.when(operationService
                        .getOperationsActByIun(Mockito.anyString()))
                .thenReturn(Mono.just(response));
        webTestClient.get()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header( PN_PAGOPA_CX_ID, "cxId")
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testWhenCalledAorTransactionByIun() {
        OperationsResponse response = new OperationsResponse();
        response.setResult(true);

        String path = "/radd-net-private/api/v1/aor/operations/by-iun/{iun}"
                .replace("{iun}", "iun-123");
        Mockito.when(operationService
                        .getOperationsAorByIun(Mockito.any()))
                .thenReturn(Mono.just(response));
        webTestClient.get()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header( PN_PAGOPA_CX_ID, "cxId")
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .exchange()
                .expectStatus().isOk();
    }


    @Test
    void testWhenCalledAorTransactionByTransactionId() {
        OperationAorResponse response = new OperationAorResponse();
        response.setElement(new OperationAorDetailResponse());

        String path = "/radd-net-private/api/v1/aor/operations/by-id/{transactionId}"
                .replace("{transactionId}", "1200");
        Mockito.when(operationService
                        .getTransactionAorByTransactionIdAndType(Mockito.anyString()))
                .thenReturn(Mono.just(response));
        webTestClient.get()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header( PN_PAGOPA_CX_ID, "cxId")
                .header( PN_PAGOPA_CX_TYPE, "PA")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testWhenCalledAorPracticesByInternalId() {
        OperationsAorDetailsResponse response = new OperationsAorDetailsResponse();
        response.setElements(List.of(new OperationAorDetailResponse()));

        String path = "/radd-net-private/api/v1/aor/operations/by-internalId/{internalId}"
                .replace("{internalId}", "internalId");
        Mockito.when(operationService
                        .getAllAorTransactionFromFiscalCode(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(response));
        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .contentType(MediaType.valueOf("application/json"))
                .bodyValue(new FilterRequest())
                .exchange()
                .expectStatus().isOk();
    }
    @Test
    void testWhenCalledActPracticesByInternalId() {
        OperationsActDetailsResponse response = new OperationsActDetailsResponse();
        response.setElements(List.of(new OperationActDetailResponse()));

        String path = "/radd-net-private/api/v1/act/operations/by-internalId/{internalId}"
                .replace("{internalId}", "internalId");
        Mockito.when(operationService
                        .getAllActTransactionFromFiscalCode(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(response));
        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .contentType(MediaType.valueOf("application/json"))
                .bodyValue(new FilterRequest())
                .exchange()
                .expectStatus().isOk();
    }
}
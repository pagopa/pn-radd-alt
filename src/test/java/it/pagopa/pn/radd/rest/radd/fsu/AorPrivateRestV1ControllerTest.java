package it.pagopa.pn.radd.rest.radd.fsu;


import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.services.radd.fsu.v1.AorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Date;


@WebFluxTest(controllers = {AorPrivateRestV1Controller.class})
class AorPrivateRestV1ControllerTest {

    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_CX_ROLE = "x-pagopa-pn-cx-role";
    public static final String PN_PAGOPA_UID = "uid";
    public static final String PN_PAGOPA_BASE_URL = "x-pagopa-pn-base-url";

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    private AorService aorService;

    @Test
    void aorInquiryTest() {
        AORInquiryResponse response = new AORInquiryResponse();
        response.setResult(true);

        String path = "/radd-net/api/v1/aor/inquiry";
        Mockito.when(aorService
                .aorInquiry(Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.eq("B2B"))
        ).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(path)
                        .queryParam("uid", "123-456")
                        .queryParam("recipientTaxId", "MRASSS90A67H718I")
                        .queryParam("recipientType", "PF").build())
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .header("x-pagopa-pn-src-ch", "B2B")
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(aorService).aorInquiry(
                Mockito.anyString(), Mockito.any(), Mockito.anyString(),
                Mockito.any(), Mockito.anyString(), Mockito.eq("B2B"));
    }

    @Test
    void startActTransactionTest() {
        StartTransactionResponse response = new StartTransactionResponse();
        StartTransactionResponseStatus status = new StartTransactionResponseStatus();
        status.setMessage("OK");
        response.status(status);

        AorStartTransactionRequest req = new AorStartTransactionRequest();
        req.setVersionToken("123TokenDocument");
        req.setFileKey("123FileKey");
        req.setOperationId("123");
        req.setRecipientTaxId("TNTGTR76E21H751S");
        req.setRecipientType(AorStartTransactionRequest.RecipientTypeEnum.valueOf("PF"));
        req.setChecksum("YTlkZGRkNzgyZWM0NzkyODdjNmQ0NGE5ZDM2YTg4ZjQ5OTE1ZGM2NjliYjgzNzViMTZhMmE5ZmE3NmE4ZDQzNwo");
        req.setOperationDate(new Date());


        String path = "/radd-net/api/v1/aor/transaction/start";
        Mockito.when(aorService
                .startTransaction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.eq("B2B"))
        ).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .header(PN_PAGOPA_CX_ROLE, "role")
                .header(PN_PAGOPA_BASE_URL, "https://example.com")
                .header("x-pagopa-pn-src-ch", "B2B")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(req), AorStartTransactionRequest.class)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(aorService).startTransaction(
                Mockito.anyString(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.eq("B2B"));
    }

    @Test
    void startAorTransactionMissingBaseUrlTest() {
        AorStartTransactionRequest req = new AorStartTransactionRequest();
        req.setVersionToken("123TokenDocument");
        req.setFileKey("123FileKey");
        req.setOperationId("123");
        req.setRecipientTaxId("TNTGTR76E21H751S");
        req.setRecipientType(AorStartTransactionRequest.RecipientTypeEnum.valueOf("PF"));
        req.setChecksum("YTlkZGRkNzgyZWM0NzkyODdjNmQ0NGE5ZDM2YTg4ZjQ5OTE1ZGM2NjliYjgzNzViMTZhMmE5ZmE3NmE4ZDQzNwo");
        req.setOperationDate(new Date());

        String path = "/radd-net/api/v1/aor/transaction/start";

        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .header(PN_PAGOPA_CX_ROLE, "role")
                // PN_PAGOPA_BASE_URL header omitted intentionally
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(req), AorStartTransactionRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.detail").isEqualTo(ExceptionTypeEnum.MISSING_BASE_URL_HEADER.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void startAorTransactionInvalidBaseUrlTest(String baseUrl) {
        AorStartTransactionRequest req = new AorStartTransactionRequest();
        req.setVersionToken("123TokenDocument");
        req.setFileKey("123FileKey");
        req.setOperationId("123");
        req.setRecipientTaxId("TNTGTR76E21H751S");
        req.setRecipientType(AorStartTransactionRequest.RecipientTypeEnum.valueOf("PF"));
        req.setChecksum("YTlkZGRkNzgyZWM0NzkyODdjNmQ0NGE5ZDM2YTg4ZjQ5OTE1ZGM2NjliYjgzNzViMTZhMmE5ZmE3NmE4ZDQzNwo");
        req.setOperationDate(new Date());

        String path = "/radd-net/api/v1/aor/transaction/start";

        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .header(PN_PAGOPA_CX_ROLE, "role")
                .header(PN_PAGOPA_BASE_URL, baseUrl)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(req), AorStartTransactionRequest.class)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.detail").isEqualTo(ExceptionTypeEnum.MISSING_BASE_URL_HEADER.getMessage());
    }

    @Test
    void completeAorTransactionTest() {
        CompleteTransactionResponse response = new CompleteTransactionResponse();
        TransactionResponseStatus status = new TransactionResponseStatus();
        status.setMessage("OK");
        response.status(status);

        CompleteTransactionRequest req = new CompleteTransactionRequest();
        req.setOperationId("123");
        req.setOperationDate(new Date());

        String path = "/radd-net/api/v1/aor/transaction/complete";
        Mockito.when(aorService
                .completeTransaction(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq("B2B"))
        ).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .header("x-pagopa-pn-src-ch", "B2B")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(req), CompleteTransactionRequest.class)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(aorService).completeTransaction(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq("B2B"));
    }

    @Test
    void abortAorTransactionTest() {
        AbortTransactionResponse response = new AbortTransactionResponse();
        TransactionResponseStatus status = new TransactionResponseStatus();
        status.setMessage("OK");
        response.status(status);

        AbortTransactionRequest req = new AbortTransactionRequest();
        req.setOperationId("123");
        req.setOperationDate(new Date());

        String path = "/radd-net/api/v1/aor/transaction/abort";
        Mockito.when(aorService.abortTransaction(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq("B2B"))
        ).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri(path)
                .header(PN_PAGOPA_UID, "myUid")
                .header(PN_PAGOPA_CX_ID, "cxId")
                .header(PN_PAGOPA_CX_TYPE, "PA")
                .header("x-pagopa-pn-src-ch", "B2B")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(req), AbortTransactionRequest.class)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(aorService).abortTransaction(
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.eq("B2B"));
    }


}

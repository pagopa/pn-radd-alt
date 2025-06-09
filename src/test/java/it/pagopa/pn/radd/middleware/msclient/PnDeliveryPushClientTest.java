package it.pagopa.pn.radd.middleware.msclient;


import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndeliverypush.v1.dto.*;
import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.exception.PnRaddException;
import it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PnDeliveryPushClientTest extends BaseTest.WithMockServer {
    @Autowired
    PnDeliveryPushClient pnDeliveryPushClient;


    @Test
    void testGetNotificationLegalFacts() {
        String recipientInternalId = "854Bgs31a", iun = "LJLH-GNTJ-DVXR-202209-J-1";
        Flux<LegalFactListElementV20Dto> fluxResponse = pnDeliveryPushClient.getNotificationLegalFacts(recipientInternalId, iun);
        fluxResponse.collectList().map(response -> {
            assertNotEquals(0, response.size());
            response.forEach(element -> {
                assertEquals("LJLH-GNTJ-DVXR-202209-J-1", element.getIun());
                assertEquals("abc", element.getLegalFactsId().getKey());
                assertEquals("1234567890", element.getTaxId());
            });
            return Mono.empty();
        }).block();
    }

    @Test
    void testGetNotificationLegalFactsWithDuplicates() {
        String recipientInternalId = "521Tvr56b", iun = "MDCA-BRSZ-UVTR-202412-G-2";
        Flux<LegalFactListElementV20Dto> fluxResponse = pnDeliveryPushClient.getNotificationLegalFacts(recipientInternalId, iun);
        fluxResponse.collectList().map(response -> {
            // The http mock for this scenario returns 2 elements, but they are duplicates so the method should return only one
            assertEquals(1, response.size());
            response.forEach(element -> {
                assertEquals("MDCA-BRSZ-UVTR-202412-G-2", element.getIun());
                assertEquals("abc", element.getLegalFactsId().getKey());
                assertEquals("1234567890", element.getTaxId());
            });
            return Mono.empty();
        }).block();
    }

    @Test
    void testGetNotificationLegalFactsCode400() {
        String recipientInternalId = "", iun = "LJLH-GNTJ-DVXR-202209-J-1";
        Flux<LegalFactListElementV20Dto> response = pnDeliveryPushClient.getNotificationLegalFacts(recipientInternalId, iun);
        response.onErrorResume(exception -> {
            if (exception instanceof PnRaddException){
                assertEquals(400, ((PnRaddException) exception).getWebClientEx().getStatusCode().value());
                return Mono.empty();
            }
            fail("Badly type exception");
            return Mono.empty();
        }).blockFirst();

    }

    @Test
    void testGetLegalFacts() {
        String recipientInternalId = "854Bgs31a", iun = "LJLH-GNTJ-DVXR-202209-J-1", legalFactId = "98765";
        Mono<LegalFactDownloadMetadataWithContentTypeResponseDto> monoResponse = pnDeliveryPushClient.getLegalFact(recipientInternalId, iun, legalFactId);
        monoResponse.map(response -> {
            assertEquals("document", response.getFilename());
            assertEquals(new BigDecimal(54092), response.getContentLength());
            assertEquals(new BigDecimal(0), response.getRetryAfter());
            assertEquals("http://down", response.getUrl());
            return Mono.empty();
        }).block();
    }

    @Test
    void testGetLegalFactsCode400() {
        String recipientInternalId = "", iun = "LJLH-GNTJ-DVXR-202209-J-1", legalFactId = "98765";
        Mono<LegalFactDownloadMetadataWithContentTypeResponseDto> monoResponse = pnDeliveryPushClient.getLegalFact(recipientInternalId, iun, legalFactId);
        monoResponse.onErrorResume(exception -> {
            if (exception instanceof PnRaddException){
                assertEquals(400, ((PnRaddException) exception).getWebClientEx().getStatusCode().value());
                return Mono.empty();
            }
            fail("Badly type exception");
            return Mono.empty();
        }).block();
    }

    @Test
    void testNotifyNotificationViewed() {
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setIun("LJLH-GNTJ-DVXR-202209-J-1");
        entity.setRecipientType(RecipientTypeDto.PF.getValue());
        entity.setOperationStartDate("2022-09-30T13:57:00.000");
        entity.setRecipientId("1924814");
        Mono<ResponseNotificationViewedDtoDto> monoResponse = pnDeliveryPushClient.notifyNotificationRaddRetrieved(entity, OffsetDateTime.now());
        monoResponse.map(response -> {
            assertNotNull(entity);
            assertEquals(entity.getIun(), response.getIun());
            return Mono.empty();
        }).block();
    }

    @Test
    void testNotifyNotificationViewedCode400() {
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setIun("LJLH-GNTJ-DVXR-202209-J-1");
        entity.setRecipientType(RecipientTypeDto.PF.getValue());
        entity.setOperationStartDate("2022-09-30T13:57:00.000");
        entity.setRecipientId("");
        Mono<ResponseNotificationViewedDtoDto> response = pnDeliveryPushClient.notifyNotificationRaddRetrieved(entity, OffsetDateTime.now());

        response.onErrorResume(exception -> {
            if (exception instanceof PnRaddException){
                assertEquals(400, ((PnRaddException) exception).getWebClientEx().getStatusCode().value());
                return Mono.empty();
            }
            fail("Badly type exception");
            return null;
        }).block();
    }

    @Test
    void testPaperNotificationFailed() {
        String recipientInternalId  = "854Bgs31a";
        Mono<List<ResponsePaperNotificationFailedDtoDto>> monoResponse = pnDeliveryPushClient.getPaperNotificationFailed(recipientInternalId).collectList();
        monoResponse.map(response -> {
            assertNotEquals(0, response.size());
            response.forEach(element -> {
                assertEquals("LJLH-GNTJ-DVXR-202209-J-1", element.getIun());
                assertEquals("854Bgs31a", element.getRecipientInternalId());
                assertEquals("http://aarUrl", element.getAarUrl());
            });
            return Mono.empty();
        }).block();
    }
    @Test
    void testPaperNotificationFailedCode400() {
        String recipientInternalId  = "854";
        Flux<ResponsePaperNotificationFailedDtoDto> response = pnDeliveryPushClient.getPaperNotificationFailed(recipientInternalId);
        response.onErrorResume(exception -> {
            if (exception instanceof PnRaddException){
                assertEquals(400, ((PnRaddException) exception).getWebClientEx().getStatusCode().value());
                return Mono.empty();
            }
            fail("Badly type exception");
            return Mono.empty();
        }).blockFirst();
    }
}

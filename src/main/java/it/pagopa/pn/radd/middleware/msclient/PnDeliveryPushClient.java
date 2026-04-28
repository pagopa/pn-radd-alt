package it.pagopa.pn.radd.middleware.msclient;

import it.pagopa.pn.commons.log.PnLogger;
import io.netty.handler.codec.http.HttpResponseStatus;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndeliverypush.v1.api.EventComunicationApi;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndeliverypush.v1.api.LegalFactsPrivateApi;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndeliverypush.v1.api.PaperNotificationFailedApi;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndeliverypush.v1.dto.*;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.PnRaddException;
import it.pagopa.pn.radd.exception.PaperNotificationFailedEmptyException;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity;
import it.pagopa.pn.radd.middleware.msclient.common.BaseClient;
import it.pagopa.pn.radd.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeoutException;

@CustomLog
@Component
@AllArgsConstructor
public class PnDeliveryPushClient extends BaseClient {
    private static final String RADD_TYPE = "ALT";
    private final EventComunicationApi eventComunicationApi;
    private final PaperNotificationFailedApi paperNotificationFailedApi;
    private final LegalFactsPrivateApi legalFactsApi;

    private final PnRaddFsuConfig pnRaddFsuConfig;


    public Flux<LegalFactListElementV20Dto> getNotificationLegalFacts(String recipientInternalId, String iun) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "getNotificationLegalFacts");
        log.debug("getNotificationLegalFacts - iun: {}, recipientInternalId: {}", iun, recipientInternalId);
        CxTypeAuthFleetDto cxType = null;
        return this.legalFactsApi.getNotificationLegalFactsPrivate( recipientInternalId, iun, null, cxType, null)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(250))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                /*
                 Eseguiamo un distinct per chiave, per rimuovere eventuali duplicati, poichè nel caso i legal facts fossero zip,
                 il flusso successivamente lancerebbe un'eccezione. Inoltre ci sembra corretto evitare di restituire più volte lo stesso legal fact.
                 */
                .distinct(legalFactListElementV20Dto -> legalFactListElementV20Dto.getLegalFactsId().getKey())
                .onErrorResume(WebClientResponseException.class, ex -> Mono.error(new PnRaddException(ex)));
    }

    public Mono<LegalFactDownloadMetadataWithContentTypeResponseDto> getLegalFact(String recipientInternalId, String iun, String legalFactId) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "getLegalFact");
        log.debug("getLegalFact - iun: {}, legalFactId: {}, recipientInternalId: {}", iun, legalFactId, recipientInternalId);
        log.trace("GET LEGAL FACT TICK {}", new Date().getTime());
        CxTypeAuthFleetDto cxType = null;
        return this.legalFactsApi.getLegalFactByIdPrivate(recipientInternalId, iun, legalFactId, null, cxType, null)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(250))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(item ->{
                    log.trace("GET LEGAL FACT TOCK {}", new Date().getTime());
                    return item;
                }).onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode().value() == HttpResponseStatus.GONE.code())
                    {
                        return Mono.error(new RaddGenericException(ExceptionTypeEnum.DOCUMENT_UNAVAILABLE));
                    }
                    return Mono.error(new PnRaddException(ex));
                });
    }

    public Mono<ResponseNotificationViewedDtoDto> notifyNotificationRaddRetrieved(RaddTransactionEntity entity, Date operationDate){
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "notifyNotificationRaddRetrieved");
        log.debug("notifyNotificationRaddRetrieved - iun: {}, operationId: {}", entity.getIun(), entity.getOperationId());
        RequestNotificationViewedDtoDto request = new RequestNotificationViewedDtoDto();
        request.setRecipientType(RecipientTypeDto.fromValue(entity.getRecipientType()));
        request.setRecipientInternalId(entity.getRecipientId());
        request.setRaddBusinessTransactionDate(DateUtils.getOffsetDateTimeFromDate(operationDate));
        request.setRaddBusinessTransactionId(entity.getOperationId());
        request.setRaddType(RADD_TYPE);
        log.trace("NOTIFICATION VIEWED TICK {}", new Date().getTime());
        return this.eventComunicationApi.notifyNotificationRaddRetrieved(entity.getIun(), request)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(item -> {
                    log.debug("response of notification viewed : {}", item.getIun());
                    log.trace("NOTIFICATION VIEWED TOCK {}", new Date().getTime());
                    return item;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.trace("NOTIFICATION VIEWED TOCK {}", new Date().getTime());
                    log.error("Notification viewed in error");
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(new PnRaddException(ex));
                });
    }


    public Flux<ResponsePaperNotificationFailedDtoDto> getPaperNotificationFailed(String recipientInternalId){
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY_PUSH, "paperNotificationFailed");
        log.debug("getPaperNotificationFailed - recipientInternalId: {}", recipientInternalId);
        return this.paperNotificationFailedApi.paperNotificationFailed(recipientInternalId, true)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND){
                        return Mono.error(new PaperNotificationFailedEmptyException());
                    }
                    return Mono.error(new PnRaddException(ex));
                });
    }

}

package it.pagopa.pn.radd.middleware.msclient;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.PnRaddException;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.microservice.msclient.generated.pndeliverypush.internal.v1.ApiClient;
import it.pagopa.pn.radd.microservice.msclient.generated.pndeliverypush.internal.v1.api.EventComunicationApi;
import it.pagopa.pn.radd.microservice.msclient.generated.pndeliverypush.internal.v1.api.PaperNotificationFailedApi;
import it.pagopa.pn.radd.microservice.msclient.generated.pndeliverypush.internal.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.radd.microservice.msclient.generated.pndeliverypush.internal.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity;
import it.pagopa.pn.radd.middleware.msclient.common.BaseClient;
import it.pagopa.pn.radd.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.NO_NOTIFICATIONS_FAILED_FOR_CF;

@Slf4j
@Component
public class PnDeliveryPushClient extends BaseClient {
    private static final String RADD_TYPE = "__FSU__";
    private EventComunicationApi eventComunicationApi;
    private TimelineAndStatusApi timelineAndStatusApi;
    private PaperNotificationFailedApi paperNotificationFailedApi;
    private final PnRaddFsuConfig pnRaddFsuConfig;

    public PnDeliveryPushClient(PnRaddFsuConfig pnRaddFsuConfig) {
        this.pnRaddFsuConfig = pnRaddFsuConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnRaddFsuConfig.getClientDeliveryPushBasepath());
        this.eventComunicationApi = new EventComunicationApi(newApiClient);
        this.timelineAndStatusApi = new TimelineAndStatusApi(newApiClient);
        this.paperNotificationFailedApi = new PaperNotificationFailedApi(newApiClient);
    }

    public Mono<NotificationHistoryResponseDto> getNotificationHistory(String iun){
        log.info("IUN : {}", iun);
        log.info("NOTIFICATION HISTORY TICK {}", new Date().getTime());
        return this.timelineAndStatusApi.getNotificationHistory(iun, 1, DateUtils.getOffsetDateTimeFromDate(new Date()))
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(item -> {
                    log.info("NOTIFICATION HISTORY TOCK {}", new Date().getTime());
                    return item;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.info("NOTIFICATION HISTORY TOCK {}", new Date().getTime());
                    ex.getStackTrace();
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(new PnRaddException(ex));
                });
    }

    public Mono<ResponseNotificationViewedDtoDto> notifyNotificationViewed(RaddTransactionEntity entity, Date operationDate){
        RequestNotificationViewedDtoDto request = new RequestNotificationViewedDtoDto();
        request.setRecipientType(RecipientTypeDto.fromValue(entity.getRecipientType()));
        request.setRecipientInternalId(entity.getRecipientId());
        request.setRaddBusinessTransactionDate(DateUtils.getOffsetDateTimeFromDate(operationDate));
        request.setRaddBusinessTransactionId(entity.getOperationId());
        request.setRaddType(RADD_TYPE);
        log.info("NOTIFICATION VIEWED TICK {}", new Date().getTime());
        return this.eventComunicationApi.notifyNotificationViewed(entity.getIun(), request)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).map(item -> {
                    log.info("NOTIFICATION VIEWED TOCK {}", new Date().getTime());
                    return item;
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.info("NOTIFICATION VIEWED TOCK {}", new Date().getTime());
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(new PnRaddException(ex));
                });
    }


    public Flux<ResponsePaperNotificationFailedDtoDto> getPaperNotificationFailed(String recipientInternalId){
        return this.paperNotificationFailedApi.paperNotificationFailed(recipientInternalId, true)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND){
                        return Mono.error(new RaddGenericException(NO_NOTIFICATIONS_FAILED_FOR_CF));
                    }
                    return Mono.error(new PnRaddException(ex));
                });
    }

}

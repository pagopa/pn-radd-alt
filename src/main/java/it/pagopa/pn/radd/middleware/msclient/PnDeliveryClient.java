package it.pagopa.pn.radd.middleware.msclient;

import io.netty.handler.codec.http.HttpResponseStatus;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.PnRaddException;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.microservice.msclient.generated.pndelivery.v1.ApiClient;
import it.pagopa.pn.radd.microservice.msclient.generated.pndelivery.v1.api.InternalOnlyApi;
import it.pagopa.pn.radd.microservice.msclient.generated.pndelivery.v1.dto.NotificationAttachmentDownloadMetadataResponseDto;
import it.pagopa.pn.radd.microservice.msclient.generated.pndelivery.v1.dto.RequestCheckAarDtoDto;
import it.pagopa.pn.radd.microservice.msclient.generated.pndelivery.v1.dto.ResponseCheckAarDtoDto;
import it.pagopa.pn.radd.microservice.msclient.generated.pndelivery.v1.dto.SentNotificationDto;
import it.pagopa.pn.radd.middleware.msclient.common.BaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class PnDeliveryClient extends BaseClient {
    private InternalOnlyApi deliveryApi;
    private final PnRaddFsuConfig pnRaddFsuConfig;


    public PnDeliveryClient(PnRaddFsuConfig pnRaddFsuConfig) {
        this.pnRaddFsuConfig = pnRaddFsuConfig;
    }

    @PostConstruct
    public void init(){
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(pnRaddFsuConfig.getClientDeliveryBasepath());
        this.deliveryApi = new InternalOnlyApi(newApiClient);
    }

    public Mono<ResponseCheckAarDtoDto> getCheckAar(String recipientType, String recipientInternalId, String qrCode) {
        RequestCheckAarDtoDto request = new RequestCheckAarDtoDto();
        request.setAarQrCodeValue(qrCode);
        request.setRecipientType(recipientType);
        request.setRecipientInternalId(recipientInternalId);
        return this.deliveryApi.checkAarQrCode(request)
                .retryWhen(
                Retry.backoff(2, Duration.ofMillis(25))
                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
        ).onErrorResume(WebClientResponseException.class, ex -> {
            log.error("Error : {}", ex.getResponseBodyAsString());
            ExceptionTypeEnum message;
            if (ex.getRawStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
                message = ExceptionTypeEnum.QR_CODE_VALIDATION;
            } else if (ex.getRawStatusCode() == HttpResponseStatus.FORBIDDEN.code()) {
                message = ExceptionTypeEnum.DOCUMENT_NOT_FOUND;
            } else if (ex.getRawStatusCode() == HttpResponseStatus.CONFLICT.code()) {
                message = ExceptionTypeEnum.ALREADY_COMPLETE_PRINT;
            } else if (ex.getRawStatusCode() == HttpResponseStatus.BAD_REQUEST.code()) {
                message = ExceptionTypeEnum.CF_OR_QRCODE_NOT_VALID;
            } else {
                return Mono.error(new PnRaddException(ex));
            }
            return Mono.error(new RaddGenericException(message));
        });
    }

    public Mono<SentNotificationDto> getNotifications(String iun){
        return this.deliveryApi.getSentNotificationPrivate(iun)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).onErrorResume(WebClientResponseException.class, ex -> Mono.error(new PnRaddException(ex)));
    }


    public Mono<NotificationAttachmentDownloadMetadataResponseDto> getPresignedUrlDocument(String iun, String docXid, String recipientTaxId){
        return this.deliveryApi.getReceivedNotificationDocumentPrivate(iun, Integer.valueOf(docXid), recipientTaxId, null)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).onErrorResume(WebClientResponseException.class, ex -> Mono.error(new PnRaddException(ex)));
    }

    public Mono<NotificationAttachmentDownloadMetadataResponseDto> getPresignedUrlPaymentDocument(String iun, String attchamentName, String recipientTaxId){
        return this.deliveryApi.getReceivedNotificationAttachmentPrivate(iun, attchamentName, recipientTaxId,null)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                ).onErrorResume(WebClientResponseException.class, ex -> Mono.error(new PnRaddException(ex)));
    }


}
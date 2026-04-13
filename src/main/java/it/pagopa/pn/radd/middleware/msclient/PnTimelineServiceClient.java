package it.pagopa.pn.radd.middleware.msclient;

import it.pagopa.pn.radd.alt.generated.openapi.msclient.pntimelineservice.v1.api.TimelineControllerApi;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pntimelineservice.v1.dto.CancellationRequestResponseDto;
import it.pagopa.pn.radd.exception.PnRaddException;
import it.pagopa.pn.radd.middleware.msclient.common.BaseClient;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@AllArgsConstructor
public class PnTimelineServiceClient extends BaseClient {
    private final TimelineControllerApi timelineControllerApi;

    public Mono<CancellationRequestResponseDto> getCancellationRequest(String iun) {
        return this.timelineControllerApi.getCancellationRequest(iun)
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(500))
                                .filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException)
                )
                .onErrorResume(WebClientResponseException.class, ex -> {
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.empty();
                    }
                    return Mono.error(new PnRaddException(ex));
                });
    }
}

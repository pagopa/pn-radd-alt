package it.pagopa.pn.radd.middleware.queue.consumer;

import it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent;
import it.pagopa.pn.radd.middleware.queue.consumer.event.PnRaddAltNormalizeRequestEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistryService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
@Configuration
@CustomLog
@RequiredArgsConstructor
public class PnRaddAltInputHandler {

    private final RegistryService registryService;

    private static final String HANDLER_REQUEST = "pnRaddAltInputHandler";
    private static final String IMPORT_COMPLETED_REQUEST = "importCompletedRequestHandler";

    @Bean
    public Consumer<Message<PnRaddAltNormalizeRequestEvent.Payload>> pnRaddAltNormalizeRequestConsumer() {
        return message -> {
            log.logStartingProcess(HANDLER_REQUEST);
            log.debug(HANDLER_REQUEST + "- message: {}", message);
            MDC.put("correlationId", message.getPayload().getCorrelationId());
            registryService.handleNormalizeRequestEvent(message.getPayload())
                    .doOnSuccess(unused -> log.logEndingProcess(HANDLER_REQUEST))
                    .doOnError(throwable ->  {
                        log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), throwable);
                    })
                    .block();
        };
    }

    @Bean
    public Consumer<Message<ImportCompletedRequestEvent.Payload>> ImportCompletedRequestConsumer() {
        return message -> {
            log.logStartingProcess(IMPORT_COMPLETED_REQUEST);
            log.debug(IMPORT_COMPLETED_REQUEST + "- message: {}", message);
            MDC.put("cxId", message.getPayload().getCxId());
            MDC.put("requestId", message.getPayload().getRequestId());
            registryService.handleImportCompletedRequest(message.getPayload())
                    .doOnSuccess(unused -> log.logEndingProcess(IMPORT_COMPLETED_REQUEST))
                    .doOnError(throwable ->  {
                        log.logEndingProcess(IMPORT_COMPLETED_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), throwable);
                    })
                    .block();
        };
    }

}

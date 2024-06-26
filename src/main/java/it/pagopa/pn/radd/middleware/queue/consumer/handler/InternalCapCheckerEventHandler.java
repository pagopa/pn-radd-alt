package it.pagopa.pn.radd.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.radd.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.radd.middleware.queue.event.PnInternalCapCheckerEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistryService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@CustomLog
public class InternalCapCheckerEventHandler {

    private RegistryService registryService;
    private static final String HANDLER_REQUEST = "pnInternalCapCheckerEventInboundConsumer";
    private static final String MDC_ZIP_CODE_KEY = "zip_code";
    @Bean
    public Consumer<Message<PnInternalCapCheckerEvent.Payload>> pnInternalCapCheckerEventInboundConsumer() {
        return message -> {
            log.debug("Handle message from {} with content {}", "Internal Cap Checker", message);
            PnInternalCapCheckerEvent.Payload payload = message.getPayload();
            MDC.put(MDC_ZIP_CODE_KEY, payload.getZipCode());

            var handleMessage = registryService.handleInternalCapCheckerMessage(payload)
                    .doOnSuccess(unused -> log.logEndingProcess(HANDLER_REQUEST))
                    .doOnError(throwable ->  {
                        log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), throwable);
                    });

            MDCUtils.addMDCToContextAndExecute(handleMessage).block();
        };
    }

}
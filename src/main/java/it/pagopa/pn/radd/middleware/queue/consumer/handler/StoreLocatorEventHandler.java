package it.pagopa.pn.radd.middleware.queue.consumer.handler;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.radd.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.radd.middleware.queue.event.RaddStoreLocatorEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.StoreLocatorService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@AllArgsConstructor
@CustomLog
public class StoreLocatorEventHandler {

    private StoreLocatorService storeLocatorService;
    private static final String HANDLER_REQUEST = "storeLocatorEventInboundConsumer";
    @Bean
    public Consumer<Message<RaddStoreLocatorEvent>> storeLocatorEventInboundConsumer() {
        return message -> {
            log.debug("Handle message with content {}", message);
            RaddStoreLocatorEvent response = message.getPayload();
            var monoResult = storeLocatorService.handleStoreLocatorEvent(response.getPayload())
                    .doOnSuccess(unused -> log.logEndingProcess(HANDLER_REQUEST))
                    .doOnError(throwable ->  {
                        log.logEndingProcess(HANDLER_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(message.getHeaders(), throwable);
                    });

            MDCUtils.addMDCToContextAndExecute(monoResult).block();
        };
    }

}

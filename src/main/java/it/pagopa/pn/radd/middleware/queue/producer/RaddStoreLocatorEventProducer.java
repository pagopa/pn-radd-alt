package it.pagopa.pn.radd.middleware.queue.producer;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.radd.middleware.queue.event.RaddStoreLocatorEvent;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static it.pagopa.pn.radd.pojo.StoreLocatorEventEnum.GENERATE;


public interface RaddStoreLocatorEventProducer extends MomProducer<RaddStoreLocatorEvent> {

    default Mono<Void> sendStoreLocatorEvent(String pk) {
        RaddStoreLocatorEvent event = buildNotification(pk);
        return Mono.fromRunnable(() -> push(event));
    }

    default RaddStoreLocatorEvent buildNotification(String pk) {
        return RaddStoreLocatorEvent.builder()
                .header(GenericEventHeader.builder()
                        .publisher(EventPublisher.RADD_ALT.name())
                        .createdAt(Instant.now())
                        .eventType("STORE_LOCATOR_EVENTS")
                        .eventId(UUID.randomUUID().toString())
                        .build()
                )
                .payload(RaddStoreLocatorEvent.Payload.builder()
                        .event(GENERATE.name())
                        .pk(pk)
                        .build()
                )
                .build();
    }
}

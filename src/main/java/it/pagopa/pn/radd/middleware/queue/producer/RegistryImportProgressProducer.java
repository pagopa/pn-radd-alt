package it.pagopa.pn.radd.middleware.queue.producer;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.radd.middleware.queue.event.RegistryImportProgressEvent;

import java.time.Instant;

public interface RegistryImportProgressProducer extends MomProducer<RegistryImportProgressEvent> {
    default void sendRegistryImportCompletedEvent(String cxId, String requestId) {
        RegistryImportProgressEvent event = buildNotification(cxId, requestId);
        this.push(event);
    }

    default RegistryImportProgressEvent buildNotification(String cxId, String requestId) {
        return RegistryImportProgressEvent.builder()
                .header(StandardEventHeader.builder()
                        .publisher("RADD_ALT") //FIXME: update when the correct value is available in EventPublisher enum
                        .createdAt(Instant.now())
                        .eventType("IMPORT_COMPLETED")
                        .build()
                )
                .payload(RegistryImportProgressEvent.Payload.builder()
                        .cxId(cxId)
                        .requestId(requestId)
                        .build()
                )
                .build();
    }
}

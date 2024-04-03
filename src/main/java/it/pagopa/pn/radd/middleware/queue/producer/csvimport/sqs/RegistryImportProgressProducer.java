package it.pagopa.pn.radd.middleware.queue.producer.csvimport.sqs;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.StandardEventHeader;

import java.time.Instant;

public interface RegistryImportProgressProducer extends MomProducer<RegistryImportProgressEvent> {
    default void sendRegistryImportCompletedEvent(String cxId, String requestId) {
        RegistryImportProgressEvent event = buildNotification(cxId, requestId);
        this.push(event);
    }

    default RegistryImportProgressEvent buildNotification(String cxId, String requestId) {
        String eventId = cxId + "#" + requestId;
        return RegistryImportProgressEvent.builder()
                .header(StandardEventHeader.builder()
                        .publisher("raddAlt")
                        .createdAt(Instant.now())
                        .eventId(eventId)
                        .eventType("importCompleted")
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

package it.pagopa.pn.radd.middleware.queue.producer.csvimport.sqs;

import it.pagopa.pn.api.dto.events.GenericFifoEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RegistryImportProgressEvent
        implements GenericFifoEvent<StandardEventHeader, RegistryImportProgressEvent.Payload> {

    private StandardEventHeader header;

    private RegistryImportProgressEvent.Payload payload;

    private String messageDeduplicationId;

    private String messageGroupId;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    @ToString
    public static class Payload {
        private String cxId;
        private String requestId;
    }
}

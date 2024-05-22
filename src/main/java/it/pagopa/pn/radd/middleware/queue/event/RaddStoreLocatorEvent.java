package it.pagopa.pn.radd.middleware.queue.event;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class RaddStoreLocatorEvent implements GenericEvent<GenericEventHeader, RaddStoreLocatorEvent.Payload> {

    private GenericEventHeader header;

    private Payload payload;

    @Getter
    @Setter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String event;
        private String pk;
    }
}

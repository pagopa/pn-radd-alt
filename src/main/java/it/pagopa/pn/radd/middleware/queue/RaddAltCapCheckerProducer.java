package it.pagopa.pn.radd.middleware.queue;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.radd.pojo.RaddAltCapCheckerEvent;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface RaddAltCapCheckerProducer extends MomProducer<RaddAltCapCheckerEvent>  {

    default Mono<Void> sendCapCheckerEvent(String zipCode ) {
        RaddAltCapCheckerEvent capCheckerEvent = buildCapCheckerEvent(zipCode);
        this.push( capCheckerEvent );
        return Mono.empty();
    }

    default RaddAltCapCheckerEvent buildCapCheckerEvent(String zipCode) {
        return RaddAltCapCheckerEvent.builder()
                .header( StandardEventHeader.builder()
                        .eventType( "CAP_CHECKER_EVENT" ) //Creare
                        .publisher( "RADD_ALT") //FIXME create RADD_ALT on EventPublisher
                        .createdAt( Instant.now() )
                        .build()
                )
                .payload( RaddAltCapCheckerEvent.Payload.builder()
                        .zipCode(zipCode)
                        .build()
                )
                .build();
    }
}

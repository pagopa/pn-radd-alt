package it.pagopa.pn.radd.middleware.queue.consumer;

import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.queue.consumer.handler.StoreLocatorEventHandler;
import it.pagopa.pn.radd.middleware.queue.event.RaddStoreLocatorEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.StoreLocatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

class StoreLocatorEventHandlerTest {

    @Mock
    private StoreLocatorService storeLocatorService;


    @Mock
    private Message<RaddStoreLocatorEvent> message;

    @InjectMocks
    private StoreLocatorEventHandler storeLocatorEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleMessageSuccessfully() {
        RaddStoreLocatorEvent event = new RaddStoreLocatorEvent();
        event.setPayload(new RaddStoreLocatorEvent.Payload("GENERATE", ""));
        when(message.getPayload()).thenReturn(event);
        when(storeLocatorService.handleStoreLocatorEvent(event.getPayload())).thenReturn(Mono.empty());

        storeLocatorEventHandler.storeLocatorEventInboundConsumer().accept(message);

        verify(storeLocatorService, times(1)).handleStoreLocatorEvent(event.getPayload());
    }

    @Test
    void shouldHandleMessageError() {
        RaddStoreLocatorEvent event = new RaddStoreLocatorEvent();
        event.setPayload(new RaddStoreLocatorEvent.Payload("GENERATE", ""));
        when(message.getPayload()).thenReturn(event);
        when(storeLocatorService.handleStoreLocatorEvent(event.getPayload())).thenReturn(Mono.error(mock(RaddGenericException.class)));
        Assertions.assertThrows(RaddGenericException.class, () -> storeLocatorEventHandler.storeLocatorEventInboundConsumer().accept(message));
    }
}
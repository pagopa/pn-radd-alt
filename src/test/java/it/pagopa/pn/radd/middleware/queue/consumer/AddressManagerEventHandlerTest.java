package it.pagopa.pn.radd.middleware.queue.consumer;

import it.pagopa.pn.radd.middleware.queue.consumer.handler.AddressManagerEventHandler;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.queue.event.AddressManagerBodyEvent;
import it.pagopa.pn.radd.middleware.queue.event.PnAddressManagerEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;
import static org.mockito.Mockito.*;

class AddressManagerEventHandlerTest {

    @Mock
    private RegistryService registryService;

    @Mock
    private Message<AddressManagerBodyEvent> message;

    @InjectMocks
    private AddressManagerEventHandler addressManagerEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleMessageSuccessfully() {
        AddressManagerBodyEvent addressManagerBodyEvent = new AddressManagerBodyEvent();
        PnAddressManagerEvent event = mock(PnAddressManagerEvent.class);
        addressManagerBodyEvent.setBody(event);
        when(event.getCorrelationId()).thenReturn("correlationId");
        when(message.getPayload()).thenReturn(addressManagerBodyEvent);
        when(registryService.handleAddressManagerEvent(event)).thenReturn(Mono.empty());

        addressManagerEventHandler.pnAddressManagerEventInboundConsumer().accept(message);

        verify(registryService, times(1)).handleAddressManagerEvent(event);
    }

    @Test
    void shouldHandleMessageError() {
        AddressManagerBodyEvent addressManagerBodyEvent = new AddressManagerBodyEvent();
        PnAddressManagerEvent event = mock(PnAddressManagerEvent.class);
        addressManagerBodyEvent.setBody(event);
        when(event.getCorrelationId()).thenReturn("correlationId");
        when(message.getPayload()).thenReturn(addressManagerBodyEvent);
        when(registryService.handleAddressManagerEvent(event)).thenReturn(Mono.error(mock(RaddGenericException.class)));

        Assertions.assertThrows(RaddGenericException.class, () -> addressManagerEventHandler.pnAddressManagerEventInboundConsumer().accept(message));

    }
}
package it.pagopa.pn.radd.middleware.queue.consumer;

import it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent;
import it.pagopa.pn.radd.middleware.queue.consumer.event.PnRaddAltNormalizeRequestEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

class RaddAltInputEventHandlerTest {

    @Mock
    private RegistryService registryService;

    @Mock
    private Message<PnRaddAltNormalizeRequestEvent.Payload> messageNormalizeRequest;

    @Mock
    private Message<ImportCompletedRequestEvent.Payload> messageImportCompleted;

    @InjectMocks
    private RaddAltInputEventHandler raddAltInputEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleNormalizeRequestSuccessfully() {
        PnRaddAltNormalizeRequestEvent.Payload event = new PnRaddAltNormalizeRequestEvent.Payload();
        when(messageNormalizeRequest.getPayload()).thenReturn(event);
        when(registryService.handleNormalizeRequestEvent(event)).thenReturn(Mono.empty());

        raddAltInputEventHandler.pnRaddAltInputNormalizeRequestConsumer().accept(messageNormalizeRequest);

        verify(registryService, times(1)).handleNormalizeRequestEvent(event);
    }

    @Test
    void shouldHandleImportCompletedSuccessfully() {
        ImportCompletedRequestEvent.Payload event = new ImportCompletedRequestEvent.Payload();
        when(registryService.handleImportCompletedRequest(event)).thenReturn(Mono.empty());

        raddAltInputEventHandler.importCompletedRequestConsumer().accept(messageImportCompleted);

        verify(registryService, times(1)).handleImportCompletedRequest(event);
    }
}
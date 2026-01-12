package it.pagopa.pn.radd.middleware.queue.consumer.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.radd.middleware.queue.event.PnRaddAltNormalizeRequestEvent;
import it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.JsonService;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Mono;
import java.util.Map;


import static org.mockito.Mockito.*;

class RaddAltInputEventHandlerTest {

    @Mock
    private RegistryService registryService;

    @Mock
    private Message<PnRaddAltNormalizeRequestEvent.Payload> messageNormalizeRequest;

    @Mock
    private Message<ImportCompletedRequestEvent.Payload> importNormalizeRequest;


    @Mock
    private Message<it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent.Payload> messageImportCompleted;

    @InjectMocks
    private RaddAltInputEventHandler raddAltInputEventHandler;

    private ObjectMapper objectMapper;
    private JsonService jsonService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        jsonService = new JsonService(objectMapper);
        raddAltInputEventHandler = new RaddAltInputEventHandler(registryService, jsonService);
    }

    @Test
    void shouldHandleNormalizeRequestSuccessfully() {
        PnRaddAltNormalizeRequestEvent.Payload event = mock(PnRaddAltNormalizeRequestEvent.Payload.class);
        when(event.getCorrelationId()).thenReturn("correlationId");
        when(messageNormalizeRequest.getPayload()).thenReturn(event);
        when(messageNormalizeRequest.getHeaders()).thenReturn(new MessageHeaders(Map.of()));

        when(registryService.handleNormalizeRequestEvent(event)).thenReturn(Mono.empty());

        raddAltInputEventHandler.pnRaddAltInputNormalizeRequestConsumer( event, messageNormalizeRequest.getHeaders());

        verify(registryService, times(1)).handleNormalizeRequestEvent(event);
    }

    @Test
    void shouldHandleImportCompletedSuccessfully() {
        it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent.Payload event = mock(it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent.Payload.class);
        when(event.getCxId()).thenReturn("cxId");
        when(event.getRequestId()).thenReturn("requestId");
        when(messageImportCompleted.getPayload()).thenReturn(event);
        when(messageImportCompleted.getHeaders()).thenReturn(new MessageHeaders(Map.of()));

        when(registryService.handleImportCompletedRequest(event)).thenReturn(Mono.empty());

        raddAltInputEventHandler.pnRaddAltImportCompletedRequestConsumer( event, messageImportCompleted.getHeaders());


        verify(registryService, times(1)).handleImportCompletedRequest(event);
    }


    @Test
    void shouldRouteNormalizeRequestEvent() throws Exception {
        PnRaddAltNormalizeRequestEvent.Payload payload = mock(PnRaddAltNormalizeRequestEvent.Payload.class);
        when(payload.getCorrelationId()).thenReturn("correlationId");

        String jsonPayload = objectMapper.writeValueAsString(payload);
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn(jsonPayload);
        when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("eventType", "RADD_NORMALIZE_REQUEST")));

        when(registryService.handleNormalizeRequestEvent(any(PnRaddAltNormalizeRequestEvent.Payload.class)))
                .thenReturn(Mono.empty());

        raddAltInputEventHandler.pnRaddAltInputMessage(message);

        verify(registryService, times(1)).handleNormalizeRequestEvent(any(PnRaddAltNormalizeRequestEvent.Payload.class));
    }


    @Test
    void shouldRouteImportCompletedEvent() throws Exception {
        ImportCompletedRequestEvent.Payload payload = mock(ImportCompletedRequestEvent.Payload.class);
        when(payload.getCxId()).thenReturn("cxId");
        when(payload.getRequestId()).thenReturn("requestId");

        String jsonPayload = objectMapper.writeValueAsString(payload);
        Message<String> message = mock(Message.class);
        when(message.getPayload()).thenReturn(jsonPayload);
        when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of("eventType", "IMPORT_COMPLETED")));

        when(registryService.handleImportCompletedRequest(any(ImportCompletedRequestEvent.Payload.class)))
                .thenReturn(Mono.empty());

        raddAltInputEventHandler.pnRaddAltInputMessage(message);

        verify(registryService, times(1)).handleImportCompletedRequest(any(ImportCompletedRequestEvent.Payload.class));
    }

}
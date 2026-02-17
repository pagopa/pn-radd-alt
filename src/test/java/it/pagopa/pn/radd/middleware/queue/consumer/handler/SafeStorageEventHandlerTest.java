package it.pagopa.pn.radd.middleware.queue.consumer.handler;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.services.radd.fsu.v1.SafeStorageEventService;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pnsafestorage.v1.dto.FileDownloadResponseDto;
import org.junit.jupiter.api.Assertions;
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

class SafeStorageEventHandlerTest {

    @Mock
    private SafeStorageEventService safeStorageEventService;

    @Mock
    private PnRaddFsuConfig pnRaddFsuConfig;

    @Mock
    private Message<FileDownloadResponseDto> message;

    @InjectMocks
    private SafeStorageEventHandler safeStorageEventHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleMessageSuccessfully() {
        FileDownloadResponseDto event = new FileDownloadResponseDto();
        event.setDocumentType("DOC_TYPE");
        when(pnRaddFsuConfig.getRegistrySafeStorageDocType()).thenReturn("DOC_TYPE");
        when(message.getPayload()).thenReturn(event);
        when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of()));

        when(safeStorageEventService.handleSafeStorageResponse(event)).thenReturn(Mono.empty());

        safeStorageEventHandler.pnSafeStorageEventInboundConsumer(message);

        verify(safeStorageEventService, times(1)).handleSafeStorageResponse(event);
    }

    @Test
    void shouldHandleMessageWithDifferentDocumentType() {
        FileDownloadResponseDto event = new FileDownloadResponseDto();
        event.setDocumentType("DOC_TYPE2");
        when(pnRaddFsuConfig.getRegistrySafeStorageDocType()).thenReturn("DOC_TYPE");
        when(message.getPayload()).thenReturn(event);
        when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of()));

        when(safeStorageEventService.handleSafeStorageResponse(event)).thenReturn(Mono.empty());

        safeStorageEventHandler.pnSafeStorageEventInboundConsumer(message);

        verify(safeStorageEventService, times(0)).handleSafeStorageResponse(event);
    }


    @Test
    void shouldHandleMessageError() {
        FileDownloadResponseDto event = new FileDownloadResponseDto();
        event.setDocumentType("DOC_TYPE");
        when(pnRaddFsuConfig.getRegistrySafeStorageDocType()).thenReturn("DOC_TYPE");
        when(message.getPayload()).thenReturn(event);
        when(message.getHeaders()).thenReturn(new MessageHeaders(Map.of()));


        when(safeStorageEventService.handleSafeStorageResponse(event)).thenReturn(Mono.error(mock(RaddGenericException.class)));
        Assertions.assertThrows(RaddGenericException.class, () -> safeStorageEventHandler.pnSafeStorageEventInboundConsumer(message));
    }
}
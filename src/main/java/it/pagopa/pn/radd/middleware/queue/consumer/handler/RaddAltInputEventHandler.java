package it.pagopa.pn.radd.middleware.queue.consumer.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.radd.middleware.queue.consumer.AbstractConsumerMessage;
import it.pagopa.pn.radd.middleware.queue.consumer.HandleEventUtils;
import it.pagopa.pn.radd.middleware.queue.event.PnRaddAltNormalizeRequestEvent;
import it.pagopa.pn.radd.services.radd.fsu.v1.RegistryService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
import it.pagopa.pn.radd.middleware.queue.consumer.event.ImportCompletedRequestEvent;

@Component
@CustomLog
@RequiredArgsConstructor
public class RaddAltInputEventHandler extends AbstractConsumerMessage {

    private final RegistryService registryService;
    private final ObjectMapper objectMapper;


    private static final String HANDLER_NORMALIZE_REQUEST = "pnRaddAltInputNormalizeRequestConsumer";

    private static final String IMPORT_COMPLETED_REQUEST = "pnRaddAltImportCompletedRequestConsumer";


    @SqsListener(value = "${pn.radd.sqs.inputQueueName}", acknowledgementMode = SqsListenerAcknowledgementMode.ALWAYS)
    public void pnRaddAltInputMessage(Message<String> message) throws JsonProcessingException {

        initTraceId(message.getHeaders());
        String eventType = message.getHeaders().get("eventType", String.class);
        MessageHeaders headers = message.getHeaders();

        if(eventType==null){
            log.warn("EventType mancante nel messaggio");
            HandleEventUtils.handleException(message.getHeaders(), new IllegalArgumentException("Missing eventType"));
            return;
        }

        switch (eventType) {
            case HANDLER_NORMALIZE_REQUEST -> {
                log.debug(HANDLER_NORMALIZE_REQUEST + "- message: {}", message);
                PnRaddAltNormalizeRequestEvent.Payload payload = objectMapper.readValue(message.getPayload(), PnRaddAltNormalizeRequestEvent.Payload.class);
                pnRaddAltInputNormalizeRequestConsumer(payload, headers);
            }
            case IMPORT_COMPLETED_REQUEST -> {
                log.debug(IMPORT_COMPLETED_REQUEST + "- message: {}", message);
                ImportCompletedRequestEvent.Payload payload = objectMapper.readValue(message.getPayload(), ImportCompletedRequestEvent.Payload.class);
                pnRaddAltImportCompletedRequestConsumer(payload, headers);
            }
            default -> {
                log.warn("EventType NON riconosciuto: {}", eventType);
                HandleEventUtils.handleException(message.getHeaders(), new IllegalArgumentException("Unknown eventType"));
            }
        }
    }

    protected void  pnRaddAltInputNormalizeRequestConsumer(PnRaddAltNormalizeRequestEvent.Payload payload, MessageHeaders headers) {
            log.logStartingProcess(HANDLER_NORMALIZE_REQUEST);

            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, payload.getCorrelationId());
            var monoResult = registryService.handleNormalizeRequestEvent(payload)
                    .doOnSuccess(unused -> log.logEndingProcess(HANDLER_NORMALIZE_REQUEST))
                    .doOnError(throwable ->  {
                        log.logEndingProcess(HANDLER_NORMALIZE_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(headers, throwable);
                    });

            MDCUtils.addMDCToContextAndExecute(monoResult).block();
        };


    protected void pnRaddAltImportCompletedRequestConsumer( ImportCompletedRequestEvent.Payload payload, MessageHeaders headers) {
            log.logStartingProcess(IMPORT_COMPLETED_REQUEST);
            MDC.put(MDCUtils.MDC_CX_ID_KEY, payload.getCxId());
            MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, payload.getRequestId());
            var monoResult = registryService.handleImportCompletedRequest(payload)
                    .doOnSuccess(unused -> log.logEndingProcess(IMPORT_COMPLETED_REQUEST))
                    .doOnError(throwable ->  {
                        log.logEndingProcess(IMPORT_COMPLETED_REQUEST, false, throwable.getMessage());
                        HandleEventUtils.handleException(headers, throwable);
                    });

            MDCUtils.addMDCToContextAndExecute(monoResult).block();
        };
    }



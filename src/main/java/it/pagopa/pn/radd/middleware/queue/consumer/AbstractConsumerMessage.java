package it.pagopa.pn.radd.middleware.queue.consumer;

import it.pagopa.pn.commons.utils.MDCUtils;
import org.slf4j.MDC;
import org.springframework.messaging.MessageHeaders;

import java.util.Objects;
import java.util.UUID;


public class AbstractConsumerMessage {


    public void initTraceId(MessageHeaders messageHeaders) {
        String traceId = null;
        String messageId = null;

        if (messageHeaders.containsKey("aws_messageId"))
            messageId = messageHeaders.get("aws_messageId", String.class);
        if (messageHeaders.containsKey("X-Amzn-Trace-Id"))
            traceId = messageHeaders.get("X-Amzn-Trace-Id", String.class);

        traceId = Objects.requireNonNullElseGet(traceId, () -> "traceId:" + UUID.randomUUID());

        MDCUtils.clearMDCKeys();
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, traceId);
        MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, messageId);
    }
}

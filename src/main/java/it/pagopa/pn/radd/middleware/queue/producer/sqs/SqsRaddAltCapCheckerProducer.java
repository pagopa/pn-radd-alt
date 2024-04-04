package it.pagopa.pn.radd.middleware.queue.producer.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.api.dto.events.PnDeliveryPaymentEvent;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.middleware.queue.RaddAltCapCheckerProducer;
import it.pagopa.pn.radd.pojo.RaddAltCapCheckerEvent;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
public class SqsRaddAltCapCheckerProducer extends AbstractSqsMomProducer<RaddAltCapCheckerEvent> implements RaddAltCapCheckerProducer {

    public SqsRaddAltCapCheckerProducer(SqsClient sqsClient, ObjectMapper objectMapper, PnRaddFsuConfig cfg) {
        super(sqsClient, cfg.getSqs().getInternalCapCheckerQueueName(), objectMapper, RaddAltCapCheckerEvent.class);
    }
}

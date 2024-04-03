package it.pagopa.pn.radd.middleware.queue.producer.csvimport.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsFifoMomProducer;
import software.amazon.awssdk.services.sqs.SqsClient;

public class RegistryImportProgressEventProducer extends AbstractSqsFifoMomProducer<RegistryImportProgressEvent> implements RegistryImportProgressProducer {

    public RegistryImportProgressEventProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper) {
        super(sqsClient, topic, objectMapper, RegistryImportProgressEvent.class);
    }
}

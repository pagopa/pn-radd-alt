package it.pagopa.pn.radd.config;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import it.pagopa.pn.radd.middleware.queue.producer.CorrelationIdEventsProducer;
import it.pagopa.pn.radd.middleware.queue.producer.RegistryImportProgressProducer;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class MockAWSObjectsTestConfig {

    @MockitoBean
    private RegistryImportProgressProducer registryImportProgressProducer;

    @MockitoBean
    private CorrelationIdEventsProducer correlationIdEventsProducer;

    @MockitoBean
    private SqsAsyncClient amazonSQS;
}

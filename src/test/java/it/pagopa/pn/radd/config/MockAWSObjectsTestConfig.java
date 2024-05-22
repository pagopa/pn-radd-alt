package it.pagopa.pn.radd.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.radd.middleware.queue.producer.CorrelationIdEventsProducer;
import it.pagopa.pn.radd.middleware.queue.producer.RaddAltCapCheckerProducer;
import it.pagopa.pn.radd.middleware.queue.producer.RaddStoreLocatorEventProducer;
import it.pagopa.pn.radd.middleware.queue.producer.RegistryImportProgressProducer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@EnableAutoConfiguration(exclude = {SqsAutoConfiguration.class})
public class MockAWSObjectsTestConfig {

    @MockBean
    private RaddAltCapCheckerProducer raddAltCapCheckerProducer;

    @MockBean
    private RegistryImportProgressProducer registryImportProgressProducer;

    @MockBean
    private CorrelationIdEventsProducer correlationIdEventsProducer;

    @MockBean
    private RaddStoreLocatorEventProducer raddStoreLocatorEventProducer;

    @MockBean
    private AmazonSQSAsync amazonSQS;
}

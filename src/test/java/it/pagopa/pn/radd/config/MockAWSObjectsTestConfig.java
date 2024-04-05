package it.pagopa.pn.radd.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import it.pagopa.pn.radd.middleware.queue.RaddAltCapCheckerProducer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

@EnableAutoConfiguration(exclude = {SqsAutoConfiguration.class})
public class MockAWSObjectsTestConfig {

    @MockBean
    private RaddAltCapCheckerProducer raddAltCapCheckerProducer;

    @MockBean
    private AmazonSQSAsync amazonSQS;
}
package it.pagopa.pn.radd.config;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
@Slf4j
public class SsmParameterConsumerActivation extends AbstractCachedSsmParameterConsumer {

    public SsmParameterConsumerActivation(SsmClient ssmClient) {
        super(ssmClient);
    }

}

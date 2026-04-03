package it.pagopa.pn.radd.middleware.msclient.config;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pntimelineservice.v1.ApiClient;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pntimelineservice.v1.api.TimelineControllerApi;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimelineServiceClientConfigurator extends CommonBaseClient {

    @Bean
    public TimelineControllerApi timelineControllerApi(PnRaddFsuConfig config) {
        ApiClient apiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(config.getClientTimelineServiceBasepath());
        return new TimelineControllerApi(apiClient);
    }
}

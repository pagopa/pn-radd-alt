package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.config.SsmParameterConsumerActivation;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.RaddStoreLocatorDAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddStoreLocatorEntity;
import it.pagopa.pn.radd.middleware.queue.event.RaddStoreLocatorEvent;
import it.pagopa.pn.radd.middleware.queue.producer.RaddStoreLocatorEventProducer;
import it.pagopa.pn.radd.pojo.StoreLocatorConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static it.pagopa.pn.radd.pojo.StoreLocatorStatusEnum.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreLocatorServiceTest {
    @Mock
    private PnRaddFsuConfig pnRaddFsuConfig;

    @Mock
    private RaddStoreLocatorDAO raddStoreLocatorDAO;

    @Mock
    private RaddStoreLocatorEventProducer raddStoreLocatorEventProducer;

    @Mock
    private SsmParameterConsumerActivation ssmParameterConsumerActivation;

    @InjectMocks
    private StoreLocatorService storeLocatorService;


    @Test
    void testHandleStoreLocatorEvent_generate() {
        RaddStoreLocatorEvent.Payload event = new RaddStoreLocatorEvent.Payload();
        event.setEvent("GENERATE");
        event.setPk("pk");
        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(event))
                .expectComplete();
    }

    @Test
    void testHandleStoreLocatorEvent_invalidevent() {
        RaddStoreLocatorEvent.Payload event = new RaddStoreLocatorEvent.Payload();
        event.setEvent("TEST");
        event.setPk("pk");
        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(event))
                .expectComplete();
    }


    @Test
    void testHandleStoreLocatorEvent_schedule_latest_uploaded_noConditionsToGenerate() {
        RaddStoreLocatorEvent.Payload payload = new RaddStoreLocatorEvent.Payload();
        payload.setEvent("SCHEDULE");

        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvConfigurationParameter("/test");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);

        StoreLocatorConfiguration storeLocatorConfiguration = new StoreLocatorConfiguration();
        storeLocatorConfiguration.setVersion("1");
        when(ssmParameterConsumerActivation.getParameterValue(storeLocator.getCsvConfigurationParameter(), StoreLocatorConfiguration.class))
                .thenReturn(Optional.of(storeLocatorConfiguration));

        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvConfigurationVersion("1");
        raddStoreLocatorEntity.setCreatedAt(Instant.now());
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(UPLOADED.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity(storeLocatorConfiguration.getVersion())).thenReturn(Mono.just(raddStoreLocatorEntity));

        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(payload))
                .verifyComplete();
    }

    @Test
    void testHandleStoreLocatorEvent_schedule_latest_notExists_sendEventToGenerate() {
        RaddStoreLocatorEvent.Payload payload = new RaddStoreLocatorEvent.Payload();
        payload.setEvent("SCHEDULE");

        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvConfigurationParameter("/test");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);

        StoreLocatorConfiguration storeLocatorConfiguration = new StoreLocatorConfiguration();
        storeLocatorConfiguration.setVersion("1");
        when(ssmParameterConsumerActivation.getParameterValue(storeLocator.getCsvConfigurationParameter(), StoreLocatorConfiguration.class))
                .thenReturn(Optional.of(storeLocatorConfiguration));


        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvConfigurationVersion("1");
        raddStoreLocatorEntity.setCreatedAt(Instant.now());
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(TO_UPLOAD.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity(storeLocatorConfiguration.getVersion())).thenReturn(Mono.empty());
        when(raddStoreLocatorDAO.putRaddStoreLocatorEntity(any())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorEventProducer.sendStoreLocatorEvent(raddStoreLocatorEntity.getPk())).thenReturn(Mono.empty());

        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(payload))
                .verifyComplete();
    }

    @Test
    void testHandleStoreLocatorEvent_schedule_latest_toUpload_sendEventToGenerate() {
        RaddStoreLocatorEvent.Payload payload = new RaddStoreLocatorEvent.Payload();
        payload.setEvent("SCHEDULE");

        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvConfigurationParameter("/test");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);
        StoreLocatorConfiguration storeLocatorConfiguration = new StoreLocatorConfiguration();
        storeLocatorConfiguration.setVersion("1");
        when(ssmParameterConsumerActivation.getParameterValue(storeLocator.getCsvConfigurationParameter(), StoreLocatorConfiguration.class))
                .thenReturn(Optional.of(storeLocatorConfiguration));


        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvConfigurationVersion("1");
        raddStoreLocatorEntity.setCreatedAt(Instant.now());
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(TO_UPLOAD.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity(storeLocatorConfiguration.getVersion())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorDAO.putRaddStoreLocatorEntity(any())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorEventProducer.sendStoreLocatorEvent(raddStoreLocatorEntity.getPk())).thenReturn(Mono.empty());

        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(payload))
                .verifyComplete();
    }

    @Test
    public void testHandleStoreLocatorEvent_schedule_latest_error_sendEventToGenerate() {
        RaddStoreLocatorEvent.Payload payload = new RaddStoreLocatorEvent.Payload();
        payload.setEvent("SCHEDULE");

        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvConfigurationParameter("/test");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);

        StoreLocatorConfiguration storeLocatorConfiguration = new StoreLocatorConfiguration();
        storeLocatorConfiguration.setVersion("1");
        when(ssmParameterConsumerActivation.getParameterValue(storeLocator.getCsvConfigurationParameter(), StoreLocatorConfiguration.class))
                .thenReturn(Optional.of(storeLocatorConfiguration));

        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvConfigurationVersion("1");
        raddStoreLocatorEntity.setCreatedAt(Instant.now());
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(ERROR.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity(storeLocatorConfiguration.getVersion())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorDAO.putRaddStoreLocatorEntity(any())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorEventProducer.sendStoreLocatorEvent(raddStoreLocatorEntity.getPk())).thenReturn(Mono.empty());

        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(payload))
                .verifyComplete();
    }

    @Test
    public void testHandleStoreLocatorEvent_schedule_latest_uploaded_sendEventToGenerate() {
        RaddStoreLocatorEvent.Payload payload = new RaddStoreLocatorEvent.Payload();
        payload.setEvent("SCHEDULE");

        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvConfigurationParameter("/test");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);

        StoreLocatorConfiguration storeLocatorConfiguration = new StoreLocatorConfiguration();
        storeLocatorConfiguration.setVersion("1");
        when(ssmParameterConsumerActivation.getParameterValue(storeLocator.getCsvConfigurationParameter(), StoreLocatorConfiguration.class))
                .thenReturn(Optional.of(storeLocatorConfiguration));


        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvConfigurationVersion("1");
        raddStoreLocatorEntity.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(UPLOADED.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity(storeLocatorConfiguration.getVersion())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorDAO.putRaddStoreLocatorEntity(any())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorEventProducer.sendStoreLocatorEvent(raddStoreLocatorEntity.getPk())).thenReturn(Mono.empty());

        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(payload))
                .verifyComplete();
    }

    @Test
    public void testHandleStoreLocatorEvent_schedule_latest_error_toRetry() {
        RaddStoreLocatorEvent.Payload payload = new RaddStoreLocatorEvent.Payload();
        payload.setEvent("SCHEDULE");

        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvConfigurationParameter("/test");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);

        StoreLocatorConfiguration storeLocatorConfiguration = new StoreLocatorConfiguration();
        storeLocatorConfiguration.setVersion("1");
        when(ssmParameterConsumerActivation.getParameterValue(storeLocator.getCsvConfigurationParameter(), StoreLocatorConfiguration.class))
                .thenReturn(Optional.of(storeLocatorConfiguration));

        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvConfigurationVersion("1");
        raddStoreLocatorEntity.setCreatedAt(Instant.now());
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(ERROR.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity(storeLocatorConfiguration.getVersion())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorDAO.putRaddStoreLocatorEntity(any())).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorEventProducer.sendStoreLocatorEvent(raddStoreLocatorEntity.getPk())).thenThrow(RuntimeException.class);

        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(payload))
                .verifyError(RuntimeException.class);
    }

    @Test
    public void testHandleStoreLocatorEvent_schedule_configuration_parameter_notFound() {
        RaddStoreLocatorEvent.Payload payload = new RaddStoreLocatorEvent.Payload();
        payload.setEvent("SCHEDULE");

        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvConfigurationParameter("/test");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);

        StoreLocatorConfiguration storeLocatorConfiguration = new StoreLocatorConfiguration();
        storeLocatorConfiguration.setVersion("1");
        when(ssmParameterConsumerActivation.getParameterValue(storeLocator.getCsvConfigurationParameter(), StoreLocatorConfiguration.class))
                .thenReturn(Optional.empty());

        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(payload))
                .verifyError(RaddGenericException.class);
    }
}


package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.middleware.db.entities.RaddStoreLocatorEntity;
import it.pagopa.pn.radd.middleware.db.impl.RaddStoreLocatorDAOImpl;
import it.pagopa.pn.radd.middleware.queue.event.RaddStoreLocatorEvent;
import it.pagopa.pn.radd.middleware.queue.producer.RaddStoreLocatorEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static it.pagopa.pn.radd.pojo.StoreLocatorStatusEnum.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreLocatorServiceTest {
    @Mock
    private PnRaddFsuConfig pnRaddFsuConfig;

    @Mock
    private RaddStoreLocatorDAOImpl raddStoreLocatorDAO;

    @Mock
    private RaddStoreLocatorEventProducer raddStoreLocatorEventProducer;

    @InjectMocks
    private StoreLocatorService storeLocatorService;

    @Test
    void testHandleStoreLocatorEvent() {
        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvType("TABLE");
        raddStoreLocatorEntity.setCreatedAt(Instant.now());
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(ERROR.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity()).thenReturn(Mono.just(raddStoreLocatorEntity));
        when(raddStoreLocatorDAO.putRaddStoreLocatorEntity(any())).thenReturn(Mono.just(raddStoreLocatorEntity));
        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvType("TABLE");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);
        RaddStoreLocatorEvent.Payload event = new RaddStoreLocatorEvent.Payload();
        event.setEvent("SCHEDULE");
        event.setPk("pk");
        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(event))
                .expectComplete();
    }

    @Test
    void testHandleStoreLocatorEvent2() {
        RaddStoreLocatorEvent.Payload event = new RaddStoreLocatorEvent.Payload();
        event.setEvent("GENERATE");
        event.setPk("pk");
        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(event))
                .expectComplete();
    }

    @Test
    void testHandleStoreLocatorEvent3() {
        RaddStoreLocatorEvent.Payload event = new RaddStoreLocatorEvent.Payload();
        event.setEvent("TEST");
        event.setPk("pk");
        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(event))
                .expectComplete();
    }

    @Test
    void testHandleStoreLocatorEvent4() {
        RaddStoreLocatorEntity raddStoreLocatorEntity = new RaddStoreLocatorEntity();
        raddStoreLocatorEntity.setPk("testPk");
        raddStoreLocatorEntity.setCsvType("TABLE");
        raddStoreLocatorEntity.setCreatedAt(Instant.now());
        raddStoreLocatorEntity.setDigest("digest");
        raddStoreLocatorEntity.setVersionId("versionId");
        raddStoreLocatorEntity.setStatus(ERROR.name());
        when(raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity()).thenReturn(Mono.empty());
        //when(raddStoreLocatorDAO.putRaddStoreLocatorEntity(any())).thenReturn(Mono.just(raddStoreLocatorEntity));
        PnRaddFsuConfig.StoreLocator storeLocator = new PnRaddFsuConfig.StoreLocator();
        storeLocator.setCsvType("TABLE");
        storeLocator.setGenerateInterval(7);
        when(pnRaddFsuConfig.getStoreLocator()).thenReturn(storeLocator);
        RaddStoreLocatorEvent.Payload event = new RaddStoreLocatorEvent.Payload();
        event.setEvent("SCHEDULE");
        event.setPk("pk");
        StepVerifier.create(storeLocatorService.handleStoreLocatorEvent(event))
                .expectComplete();
    }
}


package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.middleware.db.RaddRegistryDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryImportDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryRequestDAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryImportEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryRequestEntity;
import it.pagopa.pn.radd.middleware.msclient.PnSafeStorageClient;
import it.pagopa.pn.radd.middleware.queue.producer.csvimport.sqs.RegistryImportProgressProducer;
import it.pagopa.pn.radd.utils.ObjectMapperUtil;
import it.pagopa.pn.radd.utils.RaddRegistryUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {RegistryImportProgressService.class})
class RegistryImportProgressServiceTest {
    @Mock
    private static PnRaddFsuConfig pnRaddFsuConfig;
    @Mock
    private RaddRegistryRequestDAO raddRegistryRequestDAO;

    @Mock
    private RaddRegistryImportDAO raddRegistryImportDAO;

    @Mock
    RegistryImportProgressProducer registryImportProgressProducer;

    private void updateCfg() {
        PnRaddFsuConfig.RegistryImportProgress cfg = new PnRaddFsuConfig.RegistryImportProgress();
        cfg.setLockAtMost(1000);
        when(pnRaddFsuConfig.getRegistryImportProgress()).thenReturn(cfg);
    }

    @Test
    void testCsvImportBatchUpdateEntity() {
        String cxId = "cxId";
        String requestId = "requestId";

        updateCfg();

        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        pnRaddRegistryImportEntity.setCxId(cxId);
        pnRaddRegistryImportEntity.setRequestId(requestId);
        when(raddRegistryImportDAO.findWithStatusPending()).thenReturn(Flux.just(pnRaddRegistryImportEntity));

        RaddRegistryRequestEntity raddRegistryRequest = new RaddRegistryRequestEntity();
        when(raddRegistryRequestDAO.findByCxIdAndRequestIdAndStatusNotIn(eq(cxId), eq(requestId), anyList())).thenReturn(Flux.just(raddRegistryRequest));

        RegistryImportProgressService registryImportProgressService = new RegistryImportProgressService(raddRegistryImportDAO, raddRegistryRequestDAO, pnRaddFsuConfig, registryImportProgressProducer);
        registryImportProgressService.registryImportProgress();

        verify(raddRegistryImportDAO, never()).updateEntityToDone(any());
        verify(registryImportProgressProducer, never()).sendRegistryImportCompletedEvent(anyString(), anyString());
    }

    @Test
    void testCsvImportBatchDoNotUpdateEntity() {
        String cxId = "cxId";
        String requestId = "requestId";

        updateCfg();

        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        pnRaddRegistryImportEntity.setCxId(cxId);
        pnRaddRegistryImportEntity.setRequestId(requestId);
        when(raddRegistryImportDAO.findWithStatusPending()).thenReturn(Flux.just(pnRaddRegistryImportEntity));
        when(raddRegistryRequestDAO.findByCxIdAndRequestIdAndStatusNotIn(eq(cxId), eq(requestId), anyList())).thenReturn(Flux.empty());
        when(raddRegistryImportDAO.updateEntityToDone(any())).thenReturn(Mono.just(pnRaddRegistryImportEntity));

        RegistryImportProgressService registryImportProgressService = new RegistryImportProgressService(raddRegistryImportDAO, raddRegistryRequestDAO, pnRaddFsuConfig, registryImportProgressProducer);
        registryImportProgressService.registryImportProgress();

        verify(raddRegistryImportDAO, times(1)).updateEntityToDone(any());
        verify(registryImportProgressProducer, times(1)).sendRegistryImportCompletedEvent(anyString(), anyString());
    }

}

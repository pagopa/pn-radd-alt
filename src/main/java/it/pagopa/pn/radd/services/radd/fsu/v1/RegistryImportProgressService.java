package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.middleware.db.RaddRegistryImportDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryRequestDAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryImportEntity;
import it.pagopa.pn.radd.middleware.queue.producer.csvimport.sqs.RegistryImportProgressProducer;
import it.pagopa.pn.radd.pojo.RegistryRequestStatus;
import lombok.CustomLog;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;


@Service
@CustomLog
public class RegistryImportProgressService {
    private final RaddRegistryImportDAO registryImportDAO;
    private final RaddRegistryRequestDAO registryRequestDAO;
    private final PnRaddFsuConfig raddFsuConfig;
    private final RegistryImportProgressProducer registryImportProgressProducer;

    public RegistryImportProgressService(
            RaddRegistryImportDAO registryImportDAO,
            RaddRegistryRequestDAO registryRequestDAO,
            PnRaddFsuConfig raddFsuConfig,
            RegistryImportProgressProducer registryImportProgressProducer
    ) {
        this.registryImportDAO = registryImportDAO;
        this.registryRequestDAO = registryRequestDAO;
        this.raddFsuConfig = raddFsuConfig;
        this.registryImportProgressProducer = registryImportProgressProducer;
    }

    @Scheduled(fixedDelayString = "${pn.radd.batch-csv.delay}")
    @SchedulerLock(
            name = "pn-radd-alt-ShedLock",
            lockAtMostFor = "${pn.radd.batch-csv.lock-at-most}",
            lockAtLeastFor = "${pn.radd.batch-csv.lock-at-least}"
    )
    protected void registryImportProgressLock() {
        try {
            LockAssert.assertLocked();
            log.info("batch registryImportProgress start on: {}", LocalDateTime.now());
            registryImportProgress();
        } catch (Exception ex) {
            log.error("Exception in actionPool", ex);
        }
    }

    public void registryImportProgress() {
        StopWatch watch = StopWatch.createStarted();
        retriveAndCheckImportRequest().subscribe();
        watch.stop();
        if ((watch.getTime() / 1000) > raddFsuConfig.getRegistryImportProgress().getLockAtMost()) {
            log.warn("Time spent is greater than lockAtMostFor. Multiple nodes could schedule the same actions.");
        }
    }

    private Mono<Void> retriveAndCheckImportRequest() {
        return this.registryImportDAO.findWithStatusPending()
                .flatMap(this::checkRegistryRequest)
                .then();
    }

    private Mono<Void> checkRegistryRequest(RaddRegistryImportEntity item) {
        return this.registryRequestDAO.findByCxIdAndRequestIdAndStatusNotIn(
                        item.getCxId(),
                        item.getRequestId(),
                        List.of(RegistryRequestStatus.ACCEPTED, RegistryRequestStatus.REJECTED))
                .hasElements()
                .flatMap(hasElement -> {
                    if (Boolean.FALSE.equals(hasElement)) {
                        return this.registryImportDAO.updateEntityToDone(item)
                                .flatMap(unused -> sendSqsImportCompleted(item.getCxId(), item.getRequestId()));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> sendSqsImportCompleted(String cxId, String requestId) {
        return Mono.fromCallable(() -> {
            registryImportProgressProducer.sendRegistryImportCompletedEvent(cxId, requestId);
            return null;
        });
    }
}

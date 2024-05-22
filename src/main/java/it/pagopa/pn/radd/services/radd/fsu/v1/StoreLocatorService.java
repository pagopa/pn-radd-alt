package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.RaddStoreLocatorDAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddStoreLocatorEntity;
import it.pagopa.pn.radd.middleware.queue.event.RaddStoreLocatorEvent;
import it.pagopa.pn.radd.middleware.queue.producer.RaddStoreLocatorEventProducer;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.STORE_LOCATOR_ENTITY_NOT_FOUND;
import static it.pagopa.pn.radd.pojo.StoreLocatorEventEnum.GENERATE;
import static it.pagopa.pn.radd.pojo.StoreLocatorEventEnum.SCHEDULE;
import static it.pagopa.pn.radd.pojo.StoreLocatorStatusEnum.*;


@Service
@CustomLog
@RequiredArgsConstructor
public class StoreLocatorService {

    private final RaddStoreLocatorDAO raddStoreLocatorDAO;
    private final PnRaddFsuConfig pnRaddFsuConfig;
    private final RaddStoreLocatorEventProducer storeLocatorEventProducer;


    public Mono<Void> handleStoreLocatorEvent(RaddStoreLocatorEvent.Payload messagePayload) {
        if (GENERATE.name().equalsIgnoreCase(messagePayload.getEvent())) {
            return generateCsv()
                    .doOnError(throwable -> log.error("Error during generate csv: {}", throwable.getMessage()));
        } else if (SCHEDULE.name().equalsIgnoreCase(messagePayload.getEvent())) {
             checkConditionsAndSendGenerateEvent().block();
        }
        log.warn("Received invalid event, cannot start scheduled action payload={}", messagePayload);
        return Mono.empty();
    }

    private Mono<Void> checkConditionsAndSendGenerateEvent() {
        log.info("Start retrieve latest RaddStoreLocatorEntity having csv type: {}", pnRaddFsuConfig.getStoreLocator().getCsvType());
        return raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity()
                .switchIfEmpty(Mono.error(new RaddGenericException(STORE_LOCATOR_ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(this::checkConditionsPutEntityAndSendEvent)
                .onErrorResume(throwable -> {
                    if(throwable instanceof RaddGenericException raddGenericException
                            && HttpStatus.NOT_FOUND.equals(raddGenericException.getStatus())) {
                        log.warn("Latest RaddStoreLocatorEntity not found, start send event for generate first csv");
                        return createNewRaddStoreLocatorEntityAndSendEvent();
                    }
                    log.warn("Error during check conditions and send generate event: {}.", throwable.getMessage(), throwable);
                    return Mono.error(throwable);
                });
    }

    private Mono<Void> checkConditionsPutEntityAndSendEvent(RaddStoreLocatorEntity raddStoreLocatorEntity) {
        if (checkEntityConditions(raddStoreLocatorEntity)) {
            log.info("There are conditions to send event for generate a new csv because latest RaddStoreLocatorEntity has status: {} and createdAt: {}", raddStoreLocatorEntity.getStatus(), raddStoreLocatorEntity.getCreatedAt());
            return createNewRaddStoreLocatorEntityAndSendEvent();
        }
        log.info("There are not conditions to send event for generate a new csv because latest RaddStoreLocatorEntity has status: {} and createdAt: {}", raddStoreLocatorEntity.getStatus(), raddStoreLocatorEntity.getCreatedAt());
        return Mono.empty();
    }

    @NotNull
    private Mono<Void> createNewRaddStoreLocatorEntityAndSendEvent() {
        RaddStoreLocatorEntity storeLocatorEntityToPut = createRaddStoreLocatorEntity();
        log.info("Putting RaddStoreLocatorEntity with pk: {}", storeLocatorEntityToPut.getPk());
        return raddStoreLocatorDAO.putRaddStoreLocatorEntity(storeLocatorEntityToPut)
                .flatMap(entity -> {
                    log.info("Sending GENERATE event for Store Locator csv with pk: {}", entity.getPk());
                    return storeLocatorEventProducer.sendStoreLocatorEvent(entity.getPk());
                });
    }

    private boolean checkEntityConditions(RaddStoreLocatorEntity raddStoreLocatorEntity) {
        return ERROR.name().equals(raddStoreLocatorEntity.getStatus()) ||
                TO_UPLOAD.name().equals(raddStoreLocatorEntity.getStatus()) ||
                (UPLOADED.name().equals(raddStoreLocatorEntity.getStatus()) &&
                        raddStoreLocatorEntity.getCreatedAt()
                                .plus(pnRaddFsuConfig.getStoreLocator().getGenerateInterval(), ChronoUnit.DAYS)
                                .isBefore(Instant.now()));
    }


    private RaddStoreLocatorEntity createRaddStoreLocatorEntity() {
        RaddStoreLocatorEntity entity = new RaddStoreLocatorEntity();
        entity.setPk(UUID.randomUUID().toString());
        entity.setCreatedAt(Instant.now());
        entity.setStatus(TO_UPLOAD.name());
        entity.setCsvType(pnRaddFsuConfig.getStoreLocator().getCsvType());
        return entity;
    }

    private Mono<Void> generateCsv() {
        return Mono.empty();
    }
}

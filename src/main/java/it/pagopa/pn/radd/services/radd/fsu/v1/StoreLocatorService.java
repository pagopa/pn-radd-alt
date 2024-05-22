package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.config.SsmParameterConsumerActivation;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.RaddStoreLocatorDAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddStoreLocatorEntity;
import it.pagopa.pn.radd.middleware.queue.event.RaddStoreLocatorEvent;
import it.pagopa.pn.radd.middleware.queue.producer.RaddStoreLocatorEventProducer;
import it.pagopa.pn.radd.pojo.StoreLocatorConfiguration;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.STORE_LOCATOR_CSV_CONFIGURATION_PARAMETER_ERROR;
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
    private final SsmParameterConsumerActivation ssmParameterConsumerActivation;

    public Mono<Void> handleStoreLocatorEvent(RaddStoreLocatorEvent.Payload messagePayload) {
        if (GENERATE.name().equalsIgnoreCase(messagePayload.getEvent())) {
            log.info("Start to generate Csv");
            return generateCsv()
                    .doOnError(throwable -> log.error("Error during generate csv: {}", throwable.getMessage()));

        } else if (SCHEDULE.name().equalsIgnoreCase(messagePayload.getEvent())) {

            log.info("Start retrieve store locator csv configuration parameters");
            return ssmParameterConsumerActivation.getParameterValue(pnRaddFsuConfig.getStoreLocator().getCsvConfigurationParameter(), StoreLocatorConfiguration.class)
                    .map(this::checkConditionsAndSendGenerateEvent)
                    .orElse(Mono.error(new RaddGenericException(STORE_LOCATOR_CSV_CONFIGURATION_PARAMETER_ERROR)));
        }

        log.warn("Received invalid event, cannot start scheduled action payload={}", messagePayload);
        return Mono.empty();
    }

    private Mono<Void> checkConditionsAndSendGenerateEvent(StoreLocatorConfiguration storeLocatorConfiguration) {
        String version = storeLocatorConfiguration.getVersion();
        log.info("Start retrieve latest RaddStoreLocatorEntity having version: {}", version);
        return raddStoreLocatorDAO.retrieveLatestStoreLocatorEntity(storeLocatorConfiguration.getVersion())
                .switchIfEmpty(Mono.error(new RaddGenericException(STORE_LOCATOR_ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(this::checkConditions)
                .flatMap(raddStoreLocatorEntity -> createNewRaddStoreLocatorEntity(version))
                .flatMap(entity -> {
                    log.info("Sending GENERATE event for Store Locator csv with pk: {}", entity.getPk());
                    return storeLocatorEventProducer.sendStoreLocatorEvent(entity.getPk());
                })
                .onErrorResume(throwable -> checkIfNotFoundToSendEventForFirstGeneration(throwable, version));

    }

    private Mono<Void> checkIfNotFoundToSendEventForFirstGeneration(Throwable throwable, String version) {
        if (throwable instanceof RaddGenericException raddGenericException && HttpStatus.NOT_FOUND.equals(raddGenericException.getStatus())) {
            log.warn("Latest RaddStoreLocatorEntity not found, start send event for generate first csv");
            return createNewRaddStoreLocatorEntity(version)
                    .flatMap(entity -> {
                        log.info("Sending GENERATE event for Store Locator csv with pk: {}", entity.getPk());
                        return storeLocatorEventProducer.sendStoreLocatorEvent(entity.getPk());
                    });
        }
        log.warn("Error during check conditions and send generate event: {}.", throwable.getMessage(), throwable);
        return Mono.error(throwable);
    }

    private Mono<RaddStoreLocatorEntity> checkConditions(RaddStoreLocatorEntity raddStoreLocatorEntity) {
        if (checkEntityConditions(raddStoreLocatorEntity)) {
            log.info("There are conditions to send event for generate a new csv because latest RaddStoreLocatorEntity has status: {} and createdAt: {}", raddStoreLocatorEntity.getStatus(), raddStoreLocatorEntity.getCreatedAt());
            return Mono.just(raddStoreLocatorEntity);
        }
        log.info("There are not conditions to send event for generate a new csv because latest RaddStoreLocatorEntity has status: {} and createdAt: {}", raddStoreLocatorEntity.getStatus(), raddStoreLocatorEntity.getCreatedAt());
        return Mono.empty();
    }

    private Mono<RaddStoreLocatorEntity> createNewRaddStoreLocatorEntity(String version) {
        RaddStoreLocatorEntity storeLocatorEntityToPut = createRaddStoreLocatorEntity(version);
        log.info("Putting RaddStoreLocatorEntity with pk: {}", storeLocatorEntityToPut.getPk());
        return raddStoreLocatorDAO.putRaddStoreLocatorEntity(storeLocatorEntityToPut);
    }

    private boolean checkEntityConditions(RaddStoreLocatorEntity raddStoreLocatorEntity) {
        return ERROR.name().equals(raddStoreLocatorEntity.getStatus()) ||
                TO_UPLOAD.name().equals(raddStoreLocatorEntity.getStatus()) ||
                (UPLOADED.name().equals(raddStoreLocatorEntity.getStatus()) &&
                        raddStoreLocatorEntity.getCreatedAt()
                                .plus(pnRaddFsuConfig.getStoreLocator().getGenerateInterval(), ChronoUnit.DAYS)
                                .isBefore(Instant.now()));
    }


    private RaddStoreLocatorEntity createRaddStoreLocatorEntity(String version) {
        RaddStoreLocatorEntity entity = new RaddStoreLocatorEntity();
        entity.setPk(UUID.randomUUID().toString());
        entity.setCreatedAt(Instant.now());
        entity.setStatus(TO_UPLOAD.name());
        entity.setCsvConfigurationVersion(version);
        return entity;
    }

    private Mono<Void> generateCsv() {
        return Mono.empty();
    }
}

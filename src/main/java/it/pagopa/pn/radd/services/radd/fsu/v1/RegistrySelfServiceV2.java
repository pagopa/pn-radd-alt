package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.*;
import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.CreateRegistryRequestV2;
import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.GetRegistryResponseV2;
import it.pagopa.pn.radd.alt.generated.openapi.server.v2.dto.UpdateRegistryRequestV2;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.mapper.RaddRegistryMapper;
import it.pagopa.pn.radd.mapper.RaddRegistryPageMapper;
import it.pagopa.pn.radd.middleware.db.RaddRegistryV2DAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.services.radd.fsu.v1.validation.RegistryValidatorDispatcher;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.radd.utils.RaddRegistryUtils.*;

@Service
@RequiredArgsConstructor
@CustomLog
public class RegistrySelfServiceV2 {

    private final RaddRegistryV2DAO raddRegistryDAO;
    private final AwsGeoService awsGeoService;
    private  final RaddRegistryMapper raddRegistryMapper;
    private  final RaddRegistryPageMapper raddRegistryPageMapper;
    private final RegistryValidatorDispatcher validatorDispatcher;

    public Mono<RegistryV2> addRegistry(String partnerId, String locationId, String uid, CreateRegistryRequestV2 request) {
        log.info("Creating registry entity for partnerId: {} and locationId: {}", partnerId, locationId);
        return Mono.fromRunnable(() -> validatorDispatcher.validate(request))
                .then(Mono.defer(() -> awsGeoService.getCoordinatesForAddress(request.getAddress())))
                .map(coordinatesResult -> buildRaddRegistryEntity(partnerId, locationId, uid, request, coordinatesResult))
                .flatMap(raddRegistryDAO::putItemIfAbsent)
                .doOnNext(result -> log.debug("Registry entity with partnerId: {} and locationId: {} created successfully", partnerId, locationId))
                .map(raddRegistryMapper::toDto);
    }

    public Mono<RegistryV2> updateRegistry(String partnerId, String locationId, String uid, UpdateRegistryRequestV2 request) {
        log.info("Start updateRegistry for partnerId [{}] and locationId [{}]", partnerId, locationId);
        return Mono.fromRunnable(() -> validatorDispatcher.validate(request))
                .then(raddRegistryDAO.find(partnerId, locationId))
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.RADD_REGISTRY_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .flatMap(registryEntity -> raddRegistryDAO.updateRegistryEntity(mapFieldToUpdate(registryEntity, request, uid)))
                .map(raddRegistryMapper::toDto)
                .doOnError(throwable -> log.error("Error during update registry request for partnerId: [{}] and locationId: [{}]", partnerId, locationId, throwable));
    }


    public Mono<RegistryV2> selectiveUpdateRegistry(String partnerId, String locationId, String uid, SelectiveUpdateRegistryRequestV2 request) {
        log.info("Start selectiveUpdateRegistry for partnerId [{}] and locationId [{}]", partnerId, locationId);
        return Mono.fromRunnable(() -> validatorDispatcher.validate(request))
                .then(raddRegistryDAO.find(partnerId, locationId))
                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.RADD_REGISTRY_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .doOnNext(registryEntity -> validateAddressMatch(registryEntity.getAddress(), request.getAddress()))
                .flatMap(registryEntity -> raddRegistryDAO.updateRegistryEntity(mapFieldToSelectiveUpdate(registryEntity, request, uid)))
                .map(raddRegistryMapper::toDto)
                .doOnError(throwable -> log.error("Error during selective update registry request for partnerId: [{}] and locationId: [{}]", partnerId, locationId, throwable));
    }

    public Mono<RaddRegistryEntityV2> deleteRegistry(String partnerId, String locationId) {
        return raddRegistryDAO.delete(partnerId, locationId)
                                .switchIfEmpty(Mono.error(new RaddGenericException(ExceptionTypeEnum.RADD_REGISTRY_NOT_FOUND, HttpStatus.NOT_FOUND)))
                                .doOnNext(deletedEntity -> log.info("Registry deleted: partnerId={}, locationId={}", partnerId, locationId))
                                .doOnError(err -> log.error("Error deleting registry for partnerId={}, locationId={}", partnerId, locationId, err));
    }

    public Mono<GetRegistryResponseV2> retrieveRegistries(String partnerId, Integer limit, String lastKey) {
        log.info("start retrieveRegistry for partnerId: {}", partnerId);
        return raddRegistryDAO.findPaginatedByPartnerId(partnerId,limit, lastKey)
                .map(raddRegistryPageMapper::toDto)
                .doOnError(throwable -> log.error("Error during retrieve registry request for partnerId: {}", partnerId, throwable));
    }


}

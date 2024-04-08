package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.RegistryUploadRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.RegistryUploadResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.VerifyRequestResponse;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.RaddRegistryDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryImportDAO;
import it.pagopa.pn.radd.middleware.db.RaddRegistryRequestDAO;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryImportEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryRequestEntity;
import it.pagopa.pn.radd.middleware.msclient.PnAddressManagerClient;
import it.pagopa.pn.radd.middleware.msclient.PnSafeStorageClient;
import it.pagopa.pn.radd.middleware.queue.event.PnAddressManagerEvent;
import it.pagopa.pn.radd.middleware.queue.event.PnRaddAltNormalizeRequestEvent;
import it.pagopa.pn.radd.pojo.AddressManagerRequest;
import it.pagopa.pn.radd.pojo.ImportStatus;
import it.pagopa.pn.radd.pojo.RaddRegistryImportStatus;
import it.pagopa.pn.radd.pojo.RegistryRequestStatus;
import it.pagopa.pn.radd.utils.RaddRegistryUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.BiPredicate;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.*;
import static it.pagopa.pn.radd.utils.Const.CRUD_REGISTRY_REQUEST_ID_PREFIX;
import static it.pagopa.pn.radd.utils.Const.ERROR_DUPLICATE;

@Service
@RequiredArgsConstructor
@CustomLog
public class RegistryService {
    private final RaddRegistryRequestDAO raddRegistryRequestDAO;
    private final RaddRegistryDAO raddRegistryDAO;
    private final RaddRegistryImportDAO raddRegistryImportDAO;
    private final PnSafeStorageClient pnSafeStorageClient;
    private final RaddRegistryUtils raddRegistryUtils;
    private final PnAddressManagerClient pnAddressManagerClient;
    private final PnRaddFsuConfig pnRaddFsuConfig;


    public Mono<RegistryUploadResponse> uploadRegistryRequests(String xPagopaPnCxId, Mono<RegistryUploadRequest> registryUploadRequest) {
        String requestId = UUID.randomUUID().toString();
        return registryUploadRequest.flatMap(request ->
                raddRegistryImportDAO.getRegistryImportByCxId(xPagopaPnCxId)
                        .collectList()
                        .flatMap(entities -> checkImportRequest(request, entities))
                        .flatMap(o -> pnSafeStorageClient.createFile(raddRegistryUtils.getFileCreationRequestDto(), request.getChecksum()))
                        .flatMap(fileCreationResponseDto -> saveImportRequest(xPagopaPnCxId, request, fileCreationResponseDto, requestId).thenReturn(fileCreationResponseDto))
                        .map(fileCreationResponseDto -> mapUploadResponse(fileCreationResponseDto, requestId))
                        .doOnError(throwable -> log.error("Error uploading registry requests for cxId: {} ->", xPagopaPnCxId, throwable))
        );
    }


    private Mono<RegistryUploadRequest> checkImportRequest(RegistryUploadRequest request, List<RaddRegistryImportEntity> entities) {
        for (RaddRegistryImportEntity entity : entities) {
            if (request.getChecksum().equalsIgnoreCase(entity.getChecksum()) &&
                    (RaddRegistryImportStatus.PENDING.name().equalsIgnoreCase(entity.getStatus())
                            || (RaddRegistryImportStatus.TO_PROCESS.name().equalsIgnoreCase(entity.getStatus()) && Instant.now().isBefore(entity.getFileUploadDueDate())))) {
                return Mono.error(new RaddGenericException(ExceptionTypeEnum.valueOf(DUPLICATE_REQUEST.name()), HttpStatus.CONFLICT));
            } else if (RaddRegistryImportStatus.PENDING.name().equalsIgnoreCase(entity.getStatus())
                    || (RaddRegistryImportStatus.TO_PROCESS.name().equalsIgnoreCase(entity.getStatus()) && Instant.now().isBefore(entity.getFileUploadDueDate()))) {
                return Mono.error(new RaddGenericException(ExceptionTypeEnum.valueOf(PENDING_REQUEST.name()), HttpStatus.BAD_REQUEST));
            }
        }
        return Mono.just(request);
    }

    private RegistryUploadResponse mapUploadResponse(FileCreationResponseDto fileCreationResponseDto, String requestId) {
        RegistryUploadResponse registryUploadResponse = new RegistryUploadResponse();
        registryUploadResponse.setRequestId(requestId);
        registryUploadResponse.setFileKey(fileCreationResponseDto.getKey());
        registryUploadResponse.setUrl(fileCreationResponseDto.getUploadUrl());
        registryUploadResponse.setSecret(fileCreationResponseDto.getSecret());
        return registryUploadResponse;
    }

    private Mono<RaddRegistryImportEntity> saveImportRequest(String xPagopaPnCxId, RegistryUploadRequest request, FileCreationResponseDto fileCreationResponseDto, String requestId) {
        RaddRegistryImportEntity pnRaddRegistryImportEntity = raddRegistryUtils.getPnRaddRegistryImportEntity(xPagopaPnCxId, request, fileCreationResponseDto, requestId);
        return raddRegistryImportDAO.putRaddRegistryImportEntity(pnRaddRegistryImportEntity);
    }

    public Mono<Void> handleAddressManagerEvent(PnAddressManagerEvent message) {
        return processMessage(message.getPayload().getResultItems(), message.getPayload().getCorrelationId());
    }

    private Mono<Void> processMessage(List<PnAddressManagerEvent.ResultItem> resultItems, String correlationId) {
        String id = resultItems.get(0).getId();
        String cxId = PnAddressManagerEvent.ResultItem.retrieveCxIdFromId(id);
        String requestId = PnAddressManagerEvent.ResultItem.retrieveRequestIdFromId(id);
        return raddRegistryImportDAO.getRegistryImportByCxIdAndRequestIdFilterByStatus(cxId, requestId, ImportStatus.PENDING)
                .switchIfEmpty(Mono.error(new RaddGenericException(String.format("No pending import request found for cxId: [%s] and requestId: [%s] ", cxId, requestId))))
                .flatMap(registryImport -> raddRegistryRequestDAO.findByCorrelationIdWithStatus(correlationId, ImportStatus.PENDING)
                        .switchIfEmpty(Mono.error(new RaddGenericException("No pending items found for correlationId " + correlationId)))
                        .flatMap(raddRegistryRequest -> processAddressForRegistryRequest(resultItems, raddRegistryRequest)))
                .doOnError(throwable -> log.error("Error processing addressManager event: {}", throwable.getMessage(), throwable))
                .onErrorResume(RaddGenericException.class, e -> Mono.empty())
                .then();
    }

    private Mono<RaddRegistryRequestEntity> processAddressForRegistryRequest(List<PnAddressManagerEvent.ResultItem> resultItems, RaddRegistryRequestEntity raddRegistryRequest) {
        return raddRegistryUtils.getRelativeItemFromAddressManagerEvent(resultItems, raddRegistryRequest.getPk())
                .flatMap(item -> {
                    String error = item.getError();
                    if (StringUtils.isNotBlank(error)) {
                        log.warn("Id {} error not empty", item.getError());
                        return raddRegistryRequestDAO.updateStatusAndError(raddRegistryRequest, ImportStatus.REJECTED, error);
                    }
                    return handleRegistryUpdate(raddRegistryRequest, item);
                });
    }

    private Mono<RaddRegistryRequestEntity> handleRegistryUpdate(RaddRegistryRequestEntity raddRegistryRequestEntity, PnAddressManagerEvent.ResultItem resultItem) {
        UUID registryId = UUID.nameUUIDFromBytes(resultItem.getNormalizedAddress().toString().getBytes());

        return raddRegistryDAO.find(registryId.toString(), raddRegistryRequestEntity.getCxId())
                .flatMap(entity -> updateRegistryRequestEntity(raddRegistryRequestEntity, entity))
                .switchIfEmpty(createNewRegistryEntity(raddRegistryRequestEntity, resultItem))
                .onErrorResume(throwable -> {
                    if (throwable instanceof RaddGenericException ex && ERROR_DUPLICATE.equals(ex.getMessage())) {
                        return raddRegistryRequestDAO.updateStatusAndError(
                                raddRegistryRequestEntity,
                                ImportStatus.REJECTED,
                                ERROR_DUPLICATE
                        );
                    }
                    return Mono.error(throwable);
                });
    }

    private Mono<RaddRegistryRequestEntity> updateRegistryRequestEntity(RaddRegistryRequestEntity newRegistryRequestEntity, RaddRegistryEntity preExistingRegistryEntity) {
        if (StringUtils.equals(preExistingRegistryEntity.getRequestId(), newRegistryRequestEntity.getRequestId())) {
            return raddRegistryRequestDAO.updateStatusAndError(newRegistryRequestEntity, ImportStatus.REJECTED, ERROR_DUPLICATE);
        } else {
            return raddRegistryUtils.mergeNewRegistryEntity(preExistingRegistryEntity, newRegistryRequestEntity)
                    .flatMap(updatedEntity -> raddRegistryDAO.updateRegistryEntity(updatedEntity)
                            .flatMap(unused -> raddRegistryRequestDAO.updateRegistryRequestStatus(newRegistryRequestEntity, RegistryRequestStatus.ACCEPTED)));
        }
    }

    private Mono<RaddRegistryRequestEntity> createNewRegistryEntity(RaddRegistryRequestEntity raddRegistryRequestEntity, PnAddressManagerEvent.ResultItem resultItem) {
        return raddRegistryUtils.constructRaddRegistryEntity(resultItem.getNormalizedAddress(), raddRegistryRequestEntity)
                .flatMap(item -> this.raddRegistryDAO.putItemIfAbsent(item)
                        .onErrorResume(ConditionalCheckFailedException.class, ex -> Mono.error(new RaddGenericException(ERROR_DUPLICATE))))
                .flatMap(unused -> raddRegistryRequestDAO.updateRegistryRequestStatus(raddRegistryRequestEntity, RegistryRequestStatus.ACCEPTED));

    }
    public Mono<VerifyRequestResponse> verifyRegistriesImportRequest(String xPagopaPnCxId, String requestId) {
        log.info("start verifyRegistriesImportRequest for cxId: {} and requestId: {}", xPagopaPnCxId, requestId);
        return raddRegistryImportDAO.getRegistryImportByCxIdAndRequestId(xPagopaPnCxId, requestId)
                .switchIfEmpty(Mono.error(new RaddGenericException(IMPORT_REQUEST_NOT_FOUND, HttpStatus.NOT_FOUND)))
                .map(this::createVerifyRequestResponse)
                .doOnError(throwable -> log.error("Error during verify registries import request for cxId: [{}] and requestId: [{}]", xPagopaPnCxId, requestId, throwable));
    }
    private VerifyRequestResponse createVerifyRequestResponse(RaddRegistryImportEntity entity) {
        VerifyRequestResponse response = new VerifyRequestResponse();
        response.setRequestId(entity.getRequestId());
        response.setStatus(entity.getStatus());
        response.setError(entity.getError());
        return response;
    }

    public Mono<Void> handleNormalizeRequestEvent(PnRaddAltNormalizeRequestEvent.Payload payload) {
        AddressManagerRequest request = new AddressManagerRequest();
        request.setCorrelationId(payload.getCorrelationId());

        return raddRegistryRequestDAO.getAllFromCorrelationId(payload.getCorrelationId(), RegistryRequestStatus.NOT_WORKED.name())
                .collectList()
                .zipWhen(entities -> Mono.just(raddRegistryUtils.getRequestAddressFromOriginalRequest(entities)))
                .flatMap(tuple -> {
                    request.setAddresses(tuple.getT2());
                    String addressManagerApiKey = raddRegistryUtils.retrieveAddressManagerApiKey();
                    return pnAddressManagerClient.normalizeAddresses(request, addressManagerApiKey).thenReturn(tuple);
                })
                .flatMap(tuple -> raddRegistryRequestDAO.updateRecordsInPending(tuple.getT1()));
    }

    /**
     * The deleteOlderRegistriesAndGetZipCodeList function is used to delete older request registries and get cap list.
     *
     * @param xPagopaPnCxId Identify radd organizations
     * @param requestId RequestId of the import request just processed
     *
     * @return A flux&lt;string&gt with the zip code of the deleted registries;
     *
     */
    public Flux<String> deleteOlderRegistriesAndGetZipCodeList(String xPagopaPnCxId, String requestId) {
        log.info("start getCapListByCxIdAndRequestId for cxId: {} and requestId: {}", xPagopaPnCxId, requestId);
        return raddRegistryImportDAO.getRegistryImportByCxIdAndRequestIdFilterByStatus(xPagopaPnCxId, requestId, ImportStatus.DONE)
                .collectList().flatMapMany(raddRegistryImportEntities -> processRegistryImportsInStatusDone(xPagopaPnCxId, requestId, raddRegistryImportEntities));
    }

    /**
     * The processRegistryImportsInStatusDone function is called when the status of a registry import request is DONE.
     * It checks if there are more than one record in the database for that cxId and requestId, which means that it's not
     * the first time we receive an import request for this cxId. If so, it calls handleSubsequentImportRequest function to
     * process those records. Otherwise, it calls handleFirstImportRequest function to process them.
     * The result of these functions will be returned as a Flux&lt;String&gt; containing the zip codes of the deleted registries.
     *
     * @param xPagopaPnCxId Identify radd organizations
     * @param requestId RequestId of the import request just processed
     * @param raddRegistryImportEntities Get the first element of the list

     *
     * @return A flux&lt;string&gt;
     *
     */
    @NotNull
    private Flux<String> processRegistryImportsInStatusDone(String xPagopaPnCxId, String requestId, List<RaddRegistryImportEntity> raddRegistryImportEntities) {
        return Flux.fromIterable(raddRegistryImportEntities)
                .single()
                // If there is only one record we are handling the first import request for a cxId
                .flatMapMany(newImportRequest -> handleFirstImportRequest(xPagopaPnCxId, raddRegistryImportEntities.get(0)))
                // If we are here, we are handling more than one record (should be 2) then is a subsequent import request for a cxId
                .onErrorResume(IndexOutOfBoundsException.class, e -> Flux.defer(() -> handleSubsequentImportRequest(xPagopaPnCxId, requestId, raddRegistryImportEntities)))
                .onErrorResume(NoSuchElementException.class, e -> Flux.error(new RaddGenericException("No import request found for cxId: " + xPagopaPnCxId + " and requestId: " + requestId + " in status DONE")))
                .doOnError(throwable -> log.error("Error during processing registry imports in status DONE for cxId: [{}] and requestId: [{}]", xPagopaPnCxId, requestId, throwable));
    }

    /**
     * The handleFirstImportRequest function is called when the first import request for a given CxId arrives.
     * It checks if there are any previous requests with the same CxId made using CRUD API and deletes them, then returns
     * the zip code of the deleted registries. If no previous registries were found, it returns an empty string.

     *
     * @param xPagopaPnCxId Identify radd organizations
     * @param newRaddRegistryImportEntity The new import request entity
     * @return A flux&lt;string&gt; with the zip code of the deleted registries
     *
     */
    @NotNull
    private Flux<String> handleFirstImportRequest(String xPagopaPnCxId, RaddRegistryImportEntity newRaddRegistryImportEntity) {
        // Even if is the first import request for a cxId, we need to check if there are any previous requests with the same CxId made using CRUD API
        return raddRegistryDAO.findByCxIdAndRequestId(xPagopaPnCxId, CRUD_REGISTRY_REQUEST_ID_PREFIX)
                .collectList()
                .doOnNext(raddRegistryEntities -> log.info("Found {} registries for cxId: {} and requestId starting with: {}", raddRegistryEntities.size(), xPagopaPnCxId, CRUD_REGISTRY_REQUEST_ID_PREFIX))
                .flatMap(raddRegistryEntity -> deleteOldRegistries(raddRegistryEntity, newRaddRegistryImportEntity)
                        .thenReturn(raddRegistryEntity))
                .flatMapMany(Flux::fromIterable)
                .map(RaddRegistryEntity::getZipCode)
                .distinct();
    }

    /**
     * The handleSubsequentImportRequest function is called when a subsequent import request for the same CxId is received.
     * The function first filters out the old and new registry import entities from the list of raddRegistryImportEntities
     * It then finds all RaddRegistries for old requestId and made using CRUD API and collects them.
     * Then it deletes all the RaddRegistries and returns em.
     * Then it updates the old registry entity setting status REPLACED and TTL
     *
     * @param xPagopaPnCxId Filter the raddregistryimportentities list
     * @param newImportRequestId The new import request id
     * @param raddRegistryImportEntities raddRegistryImportEntities list (should be 2)
     *
     * @return A flux&lt;string&gt; with the zip code of the deleted registries
     *
     */
    @NotNull
    private Flux<String> handleSubsequentImportRequest(String xPagopaPnCxId, String newImportRequestId, List<RaddRegistryImportEntity> raddRegistryImportEntities) {
        RaddRegistryImportEntity oldRegistryImportEntity = filterByRequestId(newImportRequestId, raddRegistryImportEntities, registryImportWithDifferentRequestId);
        RaddRegistryImportEntity newRegistryImportEntity = filterByRequestId(newImportRequestId, raddRegistryImportEntities, registryImportWithSameRequestId);
        return raddRegistryDAO.findByCxIdAndRequestId(xPagopaPnCxId, oldRegistryImportEntity.getRequestId())
                .concatWith(raddRegistryDAO.findByCxIdAndRequestId(xPagopaPnCxId, CRUD_REGISTRY_REQUEST_ID_PREFIX))
                .collectList()
                .doOnNext(raddRegistryEntities -> log.info("Found {} registries created using CRUD API and relative to older import request for cxId: {}", raddRegistryEntities.size(), xPagopaPnCxId))
                .flatMap(raddRegistryEntities -> deleteOldRegistries(raddRegistryEntities, newRegistryImportEntity)
                        .thenReturn(raddRegistryEntities))
                .flatMap(raddRegistryEntities -> raddRegistryImportDAO.updateStatusAndTtl(oldRegistryImportEntity, getTtlForImportToReplace(), ImportStatus.REPLACED)
                        .thenReturn(raddRegistryEntities))
                .flatMapMany(Flux::fromIterable)
                .map(RaddRegistryEntity::getZipCode)
                .distinct();
    }

    private RaddRegistryImportEntity filterByRequestId(String requestId, List<RaddRegistryImportEntity> raddRegistryImportEntities, BiPredicate<RaddRegistryImportEntity, String> predicate) {
        return raddRegistryImportEntities.stream().filter(importRegistry -> predicate.test(importRegistry, requestId))
                .findFirst().orElseThrow(() -> new RaddGenericException("No import request found for requestId: " + requestId));
    }

    private final BiPredicate<RaddRegistryImportEntity, String> registryImportWithSameRequestId = (RaddRegistryImportEntity registryImport, String requestId) -> registryImport.getRequestId().equals(requestId);

    private final BiPredicate<RaddRegistryImportEntity, String> registryImportWithDifferentRequestId = (RaddRegistryImportEntity registryImport, String requestId) -> !registryImport.getRequestId().equals(requestId);

    private long getTtlForImportToReplace() {
        return Instant.now().plus(pnRaddFsuConfig.getRegistryImportReplacedTtl(), ChronoUnit.HOURS).getEpochSecond();
    }

    private Mono<Void> deleteOldRegistries(List<RaddRegistryEntity> raddRegistryEntities, RaddRegistryImportEntity raddRegistryImportEntity) {
        return Mono.empty();
    }

}

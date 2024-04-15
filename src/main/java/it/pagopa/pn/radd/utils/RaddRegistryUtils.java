package it.pagopa.pn.radd.utils;

import com.google.common.collect.Sets;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventItem;
import it.pagopa.pn.api.dto.events.PnAttachmentsConfigEventPayload;
import it.pagopa.pn.api.dto.events.PnEvaluatedZipCodeEvent;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.addressmanager.v1.dto.AnalogAddressDto;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.addressmanager.v1.dto.NormalizeItemsRequestDto;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.addressmanager.v1.dto.NormalizeRequestDto;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.middleware.db.entities.NormalizedAddressEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryImportEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryRequestEntity;
import it.pagopa.pn.radd.middleware.queue.event.PnAddressManagerEvent;
import it.pagopa.pn.radd.pojo.*;
import it.pagopa.pn.radd.services.radd.fsu.v1.SecretService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

import static it.pagopa.pn.radd.pojo.RaddRegistryImportStatus.TO_PROCESS;

@Component
@RequiredArgsConstructor
@CustomLog
public class RaddRegistryUtils {

    private final ObjectMapperUtil objectMapperUtil;
    private final PnRaddFsuConfig pnRaddFsuConfig;
    private final SecretService secretService;

    public Mono<RaddRegistryEntity> mergeNewRegistryEntity(RaddRegistryEntity preExistingRegistryEntity, RaddRegistryRequestEntity newRegistryRequestEntity) {
        return Mono.fromCallable(() -> {
            RaddRegistryOriginalRequest raddRegistryOriginalRequest = objectMapperUtil.toObject(newRegistryRequestEntity.getOriginalRequest(), RaddRegistryOriginalRequest.class);

            return getRaddRegistryEntity(preExistingRegistryEntity, newRegistryRequestEntity, raddRegistryOriginalRequest);
        });
    }

    private static RaddRegistryEntity getRaddRegistryEntity(RaddRegistryEntity preExistingRegistryEntity, RaddRegistryRequestEntity newRegistryRequestEntity, RaddRegistryOriginalRequest raddRegistryOriginalRequest) {
        RaddRegistryEntity registryEntity = new RaddRegistryEntity();

        registryEntity.setRegistryId(preExistingRegistryEntity.getRegistryId());
        registryEntity.setCxId(preExistingRegistryEntity.getCxId());
        registryEntity.setNormalizedAddress(preExistingRegistryEntity.getNormalizedAddress());
        registryEntity.setRequestId(newRegistryRequestEntity.getRequestId());
        // Metadata from originalRequest
        registryEntity.setDescription(raddRegistryOriginalRequest.getDescription());
        registryEntity.setPhoneNumber(raddRegistryOriginalRequest.getPhoneNumber());
        registryEntity.setGeoLocation(raddRegistryOriginalRequest.getGeoLocation());
        registryEntity.setZipCode(preExistingRegistryEntity.getZipCode());
        registryEntity.setOpeningTime(raddRegistryOriginalRequest.getOpeningTime());
        registryEntity.setCapacity(raddRegistryOriginalRequest.getCapacity());
        registryEntity.setExternalCode(raddRegistryOriginalRequest.getExternalCode());
        if (StringUtils.isNotBlank(raddRegistryOriginalRequest.getStartValidity())) {
            registryEntity.setStartValidity(Instant.parse(raddRegistryOriginalRequest.getStartValidity()));
        }
        if (StringUtils.isNotBlank(raddRegistryOriginalRequest.getEndValidity())) {
            registryEntity.setEndValidity(Instant.parse(raddRegistryOriginalRequest.getEndValidity()));
        }

        return registryEntity;
    }

    public Mono<RaddRegistryEntity> constructRaddRegistryEntity(String registryId, PnAddressManagerEvent.NormalizedAddress normalizedAddress, RaddRegistryRequestEntity registryRequest) {
        RaddRegistryOriginalRequest raddRegistryOriginalRequest = objectMapperUtil.toObject(registryRequest.getOriginalRequest(), RaddRegistryOriginalRequest.class);

        return Mono.just(getRaddRegistryEntity(registryId, normalizedAddress, registryRequest, raddRegistryOriginalRequest));
    }

    private static RaddRegistryEntity getRaddRegistryEntity(String registryId, PnAddressManagerEvent.NormalizedAddress normalizedAddress, RaddRegistryRequestEntity registryRequest, RaddRegistryOriginalRequest raddRegistryOriginalRequest) {
        RaddRegistryEntity registryEntity = new RaddRegistryEntity();

        registryEntity.setRegistryId(registryId);
        registryEntity.setCxId(registryRequest.getCxId());
        registryEntity.setNormalizedAddress(mapNormalizedAddressEntityToNormalizedAddress(normalizedAddress));
        registryEntity.setRequestId(registryRequest.getRequestId());
        // Metadata from originalRequest
        registryEntity.setDescription(raddRegistryOriginalRequest.getDescription());
        registryEntity.setPhoneNumber(raddRegistryOriginalRequest.getPhoneNumber());
        registryEntity.setGeoLocation(raddRegistryOriginalRequest.getGeoLocation());
        registryEntity.setZipCode(normalizedAddress.getCap());
        registryEntity.setOpeningTime(raddRegistryOriginalRequest.getOpeningTime());
        registryEntity.setCapacity(raddRegistryOriginalRequest.getCapacity());
        registryEntity.setExternalCode(raddRegistryOriginalRequest.getExternalCode());
        if (StringUtils.isNotBlank(raddRegistryOriginalRequest.getStartValidity())) {
            registryEntity.setStartValidity(Instant.parse(raddRegistryOriginalRequest.getStartValidity()));
        }
        if (StringUtils.isNotBlank(raddRegistryOriginalRequest.getEndValidity())) {
            registryEntity.setEndValidity(Instant.parse(raddRegistryOriginalRequest.getEndValidity()));
        }

        return registryEntity;
    }

    public Mono<PnAddressManagerEvent.ResultItem> getRelativeItemFromAddressManagerEvent(List<PnAddressManagerEvent.ResultItem> resultItems, String id) {
        Optional<PnAddressManagerEvent.ResultItem> resultItemOptional = resultItems.stream()
                .filter(item -> StringUtils.equals(item.getId(), RaddRegistryRequestEntity.retrieveIndexFromPk(id))).findFirst();

        if (resultItemOptional.isEmpty()) {
            log.warn("Item with id {} not found or not in event list", id);
            return Mono.empty();
        }
        return Mono.just(resultItemOptional.get());
    }

    public RaddRegistryImportEntity getPnRaddRegistryImportEntity(String xPagopaPnCxId, RegistryUploadRequest request, FileCreationResponseDto fileCreationResponseDto, String requestId) {
        RaddRegistryImportEntity pnRaddRegistryImportEntity = new RaddRegistryImportEntity();
        pnRaddRegistryImportEntity.setRequestId(requestId);
        pnRaddRegistryImportEntity.setStatus(TO_PROCESS.name());
        pnRaddRegistryImportEntity.setChecksum(request.getChecksum());
        pnRaddRegistryImportEntity.setCxId(xPagopaPnCxId);
        pnRaddRegistryImportEntity.setFileKey(fileCreationResponseDto.getKey());
        pnRaddRegistryImportEntity.setCreatedAt(Instant.now());
        pnRaddRegistryImportEntity.setUpdatedAt(Instant.now());
        pnRaddRegistryImportEntity.setFileUploadDueDate(Instant.now().plus(pnRaddFsuConfig.getRegistryImportUploadFileTtl(), ChronoUnit.SECONDS));

        RaddRegistryImportConfig raddRegistryImportConfig = new RaddRegistryImportConfig();
        raddRegistryImportConfig.setDeleteRole(pnRaddFsuConfig.getRegistryDefaultDeleteRule());
        raddRegistryImportConfig.setDefaultEndValidity(pnRaddFsuConfig.getRegistryDefaultEndValidity());
        pnRaddRegistryImportEntity.setConfig(objectMapperUtil.toJson(raddRegistryImportConfig));

        return pnRaddRegistryImportEntity;
    }

    public FileCreationRequestDto getFileCreationRequestDto() {
        FileCreationRequestDto request = new FileCreationRequestDto();
        request.setStatus(Const.SAVED);
        request.setContentType(Const.CONTENT_TYPE_TEXT_CSV);
        request.setDocumentType(this.pnRaddFsuConfig.getRegistrySafeStorageDocType());
        return request;
    }

    public List<AddressManagerRequestAddress> getRequestAddressFromOriginalRequest(List<RaddRegistryRequestEntity> entities) {
        return entities.stream().map(entity -> {
            AddressManagerRequestAddress request = objectMapperUtil.toObject(entity.getOriginalRequest(), AddressManagerRequestAddress.class);
            request.setId(RaddRegistryRequestEntity.retrieveIndexFromPk(entity.getPk()));
            return request;
        }).toList();
    }

    public NormalizeItemsRequestDto getNormalizeRequestDtoFromAddressManagerRequest(AddressManagerRequest request) {
        NormalizeItemsRequestDto requestDto = new NormalizeItemsRequestDto();
        requestDto.setCorrelationId(request.getCorrelationId());
        List<NormalizeRequestDto> listDto = request.getAddresses().stream().map(getAddressManagerRequestAddressNormalizeRequestDto()).toList();
        requestDto.setRequestItems(listDto);
        return requestDto;
    }

    private static Function<AddressManagerRequestAddress, NormalizeRequestDto> getAddressManagerRequestAddressNormalizeRequestDto() {
        return address -> {
            NormalizeRequestDto dto = new NormalizeRequestDto();
            dto.setId(address.getId());
            AnalogAddressDto addressDto = mapAddress(address);
            dto.setAddress(addressDto);
            return dto;
        };
    }

    private static AnalogAddressDto mapAddress(AddressManagerRequestAddress address) {
        AnalogAddressDto addressDto = new AnalogAddressDto();
        addressDto.setAddressRow(address.getAddressRow());
        addressDto.setCap(address.getCap());
        addressDto.setCity(address.getCity());
        addressDto.setPr(address.getPr());
        addressDto.setCountry(address.getCountry());
        return addressDto;
    }

    public String retrieveAddressManagerApiKey() {
        return secretService.getSecret(pnRaddFsuConfig.getAddressManagerApiKeySecret());
    }

    public PnEvaluatedZipCodeEvent mapToEventMessage(Set<TimeInterval> timeIntervals, String zipCode) {
        return PnEvaluatedZipCodeEvent.builder().detail(
                PnAttachmentsConfigEventPayload
                        .builder()
                        .configKey(zipCode)
                        .configType(pnRaddFsuConfig.getEvaluatedZipCodeConfigType())
                        .configs(getConfigEntries(timeIntervals))
                        .build()
        ).build();
    }

    private List<PnAttachmentsConfigEventItem> getConfigEntries(Set<TimeInterval> timeIntervals) {
        return timeIntervals.stream()
                .map(timeInterval -> PnAttachmentsConfigEventItem.builder()
                        .startValidity(timeInterval.getStart())
                        .endValidity(timeInterval.getEnd() == Instant.MAX ? null : timeInterval.getEnd())
                        .build()).toList();
    }

    public List<TimeInterval> getOfficeIntervals(List<RaddRegistryEntity> raddRegistryEntities) {
        return raddRegistryEntities.stream()
                .map(raddRegistryEntity -> {
                    if (raddRegistryEntity.getEndValidity() == null) {
                        return new TimeInterval(raddRegistryEntity.getStartValidity(), Instant.MAX);
                    } else {
                        return new TimeInterval(raddRegistryEntity.getStartValidity(), raddRegistryEntity.getEndValidity());
                    }
                }).toList();
    }


    /**
     * Finds active time intervals from a list of time intervals based on the EvaluatedZipCodeConfigNumber
     * parameter that indicates the minimum number of registries that has to be active to consider an interval retrievable.
     *
     * @param timeIntervals List of time intervals to evaluate.
     * @return Set of active time intervals.
     */
    public Set<TimeInterval> findActiveIntervals(List<TimeInterval> timeIntervals) {
        Set<TimeInterval> activeIntervals; // Set to store active time intervals

        // If evaluated zip code configuration number is equal to number of time intervals,
        // find intersection of all time intervals. In this case, we don't need to calculate any other combinations
        // other than the one which involves every registry in the set, so we proceed directly to find the intersection
        // of time between each registry.
        if (pnRaddFsuConfig.getEvaluatedZipCodeConfigNumber() == timeIntervals.size()) {
            TimeInterval timeInterval = findIntersection(timeIntervals);
            // Return single active interval if intersection exists, otherwise an empty set
            return timeInterval != null ? Set.of(timeInterval) : Set.of();
        }
        // Check if evaluated zip code configuration number is not 1. If it's not 1 we need to calculate all the
        // possible combinations of given number of registries (EvaluatedZipCodeConfigNumber) to then try to find an intersection
        // between the time intervals of each subset.
        else if (pnRaddFsuConfig.getEvaluatedZipCodeConfigNumber() != 1 && pnRaddFsuConfig.getEvaluatedZipCodeConfigNumber() < timeIntervals.size()) {
            // Generate combinations of time intervals based on evaluated configuration number
            Set<Set<TimeInterval>> result = Sets.combinations(new HashSet<>(timeIntervals), pnRaddFsuConfig.getEvaluatedZipCodeConfigNumber());

            activeIntervals = new HashSet<>(); // Initialize set to store active intervals
            // Iterate through each combination of time intervals
            for (Set<TimeInterval> intervalSet : result) {
                // Find intersection of time intervals in the combination
                TimeInterval timeInterval = findIntersection(intervalSet.stream().toList());
                // If intersection exists, add it to active intervals set
                if (timeInterval != null) {
                    activeIntervals.add(timeInterval);
                }
            }
        } else {
            // If evaluated zip code configuration number is 1 and not equal to size of time intervals,
            // consider all time intervals as active. In this case we just need to take into consideration the n subset
            // made of just one element. In other words, we just need to merge the intervals of each registry, since we just need
            // one active registry.
            activeIntervals = new HashSet<>(timeIntervals);
        }

        // Merge overlapping intervals and return the result
        return mergeIntervals(new ArrayList<>(activeIntervals));
    }


    /**
     * Finds the intersection of a list of time intervals.
     *
     * @param intervals List of time intervals to find intersection from.
     * @return TimeInterval representing the intersection, or null if no intersection exists.
     */
    static TimeInterval findIntersection(List<TimeInterval> intervals) {
        // Initialize start and end times with first interval's start and end times
        Instant start = intervals.get(0).getStart();
        Instant end = intervals.get(0).getEnd();

        // Iterate through the rest of the intervals
        for (int i = 1; i < intervals.size(); i++) {
            // If the current interval starts after the current end time or ends before the current start time,
            // there's no intersection, return null
            if (intervals.get(i).getStart().isAfter(end) || intervals.get(i).getEnd().isBefore(start)) {
                return null;
            } else {
                // Update start and end times to reflect the intersection
                if (start.isBefore(intervals.get(i).getStart()))
                    start = intervals.get(i).getStart();
                if (end.isAfter(intervals.get(i).getEnd()))
                    end = intervals.get(i).getEnd();
            }
        }
        // Return the TimeInterval representing the intersection
        return new TimeInterval(start, end);
    }

    /**
     * Merges overlapping time intervals into a consolidated set of non-overlapping intervals.
     *
     * @param timeIntervals List of time intervals to merge.
     * @return Set of merged time intervals.
     */
    public static Set<TimeInterval> mergeIntervals(List<TimeInterval> timeIntervals) {
        // If the list of intervals is empty, return an empty set
        if (timeIntervals.isEmpty()) {
            return Set.of();
        }

        // Sort the time intervals based on their start times
        timeIntervals.sort(Comparator.comparing(TimeInterval::getStart));

        Stack<TimeInterval> stack = new Stack<>(); // Stack to store merged intervals
        stack.push(timeIntervals.get(0)); // Push the first interval onto the stack

        // Iterate through the rest of the intervals
        for (int i = 1; i < timeIntervals.size(); i++) {
            TimeInterval top = stack.peek(); // Get the top interval from the stack

            // If the current interval starts after the end of the top interval on the stack,
            // push the current interval onto the stack
            if (top.getEnd().isBefore(timeIntervals.get(i).getStart()))
                stack.push(timeIntervals.get(i));
                // If the current interval's end time is after the end time of the top interval on the stack,
                // merge the intervals by updating the end time of the top interval
            else if (top.getEnd().isBefore(timeIntervals.get(i).getEnd())) {
                top.setEnd(timeIntervals.get(i).getEnd());
                stack.pop(); // Pop the top interval from the stack
                stack.push(top); // Push the merged interval onto the stack
            }
        }

        // Convert the stack of intervals to a set and actualize any past intervals
        TimeInterval[] activeIntervals = new TimeInterval[0];
        return actualizePastIntervals(Set.of(stack.toArray(activeIntervals)));
    }

    private static Set<TimeInterval> actualizePastIntervals(Set<TimeInterval> timeIntervals) {
        /* At this point, we should have only active intervals ranging from before today to an indefinite future time.
        If the interval starts before today, we update it with today's date. */
        Instant now = getStartOfTodayInstant();
        for (TimeInterval timeInterval : timeIntervals) {
            if (timeInterval.getStart().isBefore(now)) {
                timeInterval.setStart(now);
            }
        }

        return timeIntervals;
    }

    private static Instant getStartOfTodayInstant() {
        return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public RequestResponse mapToRequestResponse(ResultPaginationDto<RaddRegistryRequestEntity, String> resultPaginationDto) {
        RequestResponse result = new RequestResponse();
        if (resultPaginationDto.getResultsPage() != null) {
            result.setItems(resultPaginationDto.getResultsPage().stream()
                    .map(mapRaddRegistryRequestResponse())
                    .toList());
        }
        result.setNextPagesKey(resultPaginationDto.getNextPagesKey());
        result.setMoreResult(resultPaginationDto.isMoreResult());
        return result;
    }

    private @NotNull Function<RaddRegistryRequestEntity, RegistryRequestResponse> mapRaddRegistryRequestResponse() {
        return raddRegistryRequestEntity -> {
            RegistryRequestResponse registryRequestResponse = new RegistryRequestResponse();
            registryRequestResponse.setRegistryId(raddRegistryRequestEntity.getRegistryId());
            registryRequestResponse.setRequestId(raddRegistryRequestEntity.getRequestId());
            registryRequestResponse.setError(raddRegistryRequestEntity.getError());
            registryRequestResponse.setCreatedAt(raddRegistryRequestEntity.getCreatedAt().toString());
            registryRequestResponse.setUpdatedAt(raddRegistryRequestEntity.getUpdatedAt().toString());
            registryRequestResponse.setStatus(raddRegistryRequestEntity.getStatus());
            RaddRegistryOriginalRequest originalRequest = objectMapperUtil.toObject(raddRegistryRequestEntity.getOriginalRequest(), RaddRegistryOriginalRequest.class);
            registryRequestResponse.setOriginalRequest(convertToOriginalRequest(originalRequest));

            return registryRequestResponse;
        };
    }

    private OriginalRequest convertToOriginalRequest(RaddRegistryOriginalRequest raddRegistryOriginalRequest) {
        OriginalRequest originalRequest = new OriginalRequest();

        if (raddRegistryOriginalRequest == null) {
            return originalRequest;
        }

        originalRequest.setOriginalAddress(convertToAddress(raddRegistryOriginalRequest));
        originalRequest.setDescription(raddRegistryOriginalRequest.getDescription());
        originalRequest.setPhoneNumber(raddRegistryOriginalRequest.getPhoneNumber());
        try {
            OriginalRequestGeoLocation geoLocation = new OriginalRequestGeoLocation();
            if (StringUtils.isNotBlank(raddRegistryOriginalRequest.getGeoLocation())) {
                geoLocation = objectMapperUtil.toObject(raddRegistryOriginalRequest.getGeoLocation(), OriginalRequestGeoLocation.class);
            }
            originalRequest.setGeoLocation(geoLocation);
        } catch (PnInternalException e) {
            log.debug("There are no valid geolocation data for this registry request.");
        }
        originalRequest.setOpeningTime(raddRegistryOriginalRequest.getOpeningTime());
        if (StringUtils.isNotBlank(raddRegistryOriginalRequest.getStartValidity())) {
            Instant instant = Instant.parse(raddRegistryOriginalRequest.getStartValidity());
            originalRequest.setStartValidity(Date.from(instant));
        }
        if (StringUtils.isNotBlank(raddRegistryOriginalRequest.getEndValidity())) {
            Instant instant = Instant.parse(raddRegistryOriginalRequest.getEndValidity());
            originalRequest.setEndValidity(Date.from(instant));
        }
        originalRequest.setCapacity(raddRegistryOriginalRequest.getCapacity());
        originalRequest.setExternalCode(raddRegistryOriginalRequest.getExternalCode());
        return originalRequest;
    }

    private Address convertToAddress(RaddRegistryOriginalRequest raddRegistryOriginalRequest) {
        Address address = new Address();
        address.setAddressRow(raddRegistryOriginalRequest.getAddressRow());
        address.setCap(raddRegistryOriginalRequest.getCap());
        address.setCity(raddRegistryOriginalRequest.getCity());
        address.setPr(raddRegistryOriginalRequest.getPr());
        address.setCountry(raddRegistryOriginalRequest.getCountry());
        return address;
    }

    public RegistriesResponse mapRegistryEntityToRegistry(ResultPaginationDto<RaddRegistryEntity, String> resultPaginationDto) {
        RegistriesResponse result = new RegistriesResponse();
        if (resultPaginationDto.getResultsPage() != null) {
            result.setRegistries(resultPaginationDto.getResultsPage().stream()
                    .map(entity -> {
                        Registry registry = new Registry();
                        registry.setRegistryId(entity.getRegistryId());
                        registry.setRequestId(entity.getRequestId());
                        registry.setAddress(mapNormalizedAddressToAddress(entity.getNormalizedAddress()));
                        registry.setDescription(entity.getDescription());
                        registry.setPhoneNumber(entity.getPhoneNumber());
                        try {
                            if (StringUtils.isNotBlank(entity.getGeoLocation())) {
                                CreateRegistryRequestGeoLocation geoLocation = objectMapperUtil.toObject(entity.getGeoLocation(), CreateRegistryRequestGeoLocation.class);
                                geoLocation.setLatitude(geoLocation.getLatitude());
                                geoLocation.setLongitude(geoLocation.getLatitude());
                                registry.setGeoLocation(geoLocation);
                            }
                        } catch (PnInternalException e) {
                            log.debug("Registry with cxId = {} and registryId = {} has not valid geoLocation", entity.getCxId(), entity.getRegistryId(), e);
                        }
                        registry.setOpeningTime(entity.getOpeningTime());
                        registry.setStartValidity(Date.from(entity.getStartValidity()));
                        if (entity.getEndValidity() != null) {
                            registry.setEndValidity(Date.from(entity.getEndValidity()));
                        }
                        registry.setExternalCode(entity.getExternalCode());
                        registry.setCapacity(entity.getCapacity());
                        return registry;
                    })
                    .toList());
        }
        result.setNextPagesKey(resultPaginationDto.getNextPagesKey());
        result.setMoreResult(resultPaginationDto.isMoreResult());
        return result;
    }

    private Address mapNormalizedAddressToAddress(NormalizedAddressEntity normalizedAddress) {
        Address address = new Address();
        address.addressRow(normalizedAddress.getAddressRow());
        address.cap(normalizedAddress.getCap());
        address.pr(normalizedAddress.getPr());
        address.city(normalizedAddress.getCity());
        address.country(normalizedAddress.getCountry());
        return address;
    }

    private static NormalizedAddressEntity mapNormalizedAddressEntityToNormalizedAddress(PnAddressManagerEvent.NormalizedAddress normalizedAddress) {
        NormalizedAddressEntity address = new NormalizedAddressEntity();
        address.setAddressRow(normalizedAddress.getAddressRow());
        address.setCap(normalizedAddress.getCap());
        address.setPr(normalizedAddress.getPr());
        address.setCity(normalizedAddress.getCity());
        address.setCountry(normalizedAddress.getCountry());
        return address;
    }

}

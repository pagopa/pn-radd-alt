package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.RegistryV2;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.StoreRegistriesResponse;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddRegistryEntityV2;
import it.pagopa.pn.radd.pojo.PnLastEvaluatedKey;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@CustomLog
public class StoreRegistriesMapper {

    private final RaddRegistryMapper raddRegistryMapper;

    private final static Function<Map<String, AttributeValue>, PnLastEvaluatedKey> STORE_REGISTRY_LAST_EVALUATED_KEY = (stringAttributeValueMap) -> {
        PnLastEvaluatedKey pageLastEvaluatedKey = new PnLastEvaluatedKey();
        pageLastEvaluatedKey.setExternalLastEvaluatedKey(stringAttributeValueMap.get(RaddRegistryEntityV2.COL_LOCATION_ID).s());
        pageLastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                RaddRegistryEntityV2.COL_PARTNER_ID, AttributeValue.builder().s(stringAttributeValueMap.get(RaddRegistryEntityV2.COL_PARTNER_ID).s()).build(),
                RaddRegistryEntityV2.COL_LOCATION_ID, AttributeValue.builder().s(stringAttributeValueMap.get(RaddRegistryEntityV2.COL_LOCATION_ID).s()).build()
        ));
        return pageLastEvaluatedKey;
    };

    public StoreRegistriesResponse mapToStoreRegistriesResponse(Page<RaddRegistryEntityV2> registries) {
        StoreRegistriesResponse storeRegistriesResponse = new StoreRegistriesResponse();
        storeRegistriesResponse.setRegistries(mapRegistryEntityToRegistryStore(registries.items()));
        if (registries.lastEvaluatedKey() != null) {
            storeRegistriesResponse.setLastKey(STORE_REGISTRY_LAST_EVALUATED_KEY.apply(registries.lastEvaluatedKey()).serializeInternalLastEvaluatedKey());
        }
        log.info("StoreRegistriesResponse created with {} registries", storeRegistriesResponse.getRegistries().size());
        return storeRegistriesResponse;
    }

    public List<RegistryV2> mapRegistryEntityToRegistryStore(List<RaddRegistryEntityV2> raddRegistryEntities) {
        return raddRegistryEntities.stream()
                .map(raddRegistryMapper::toDto)
                .toList();
    }

}

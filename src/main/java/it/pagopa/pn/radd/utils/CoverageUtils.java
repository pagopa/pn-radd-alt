package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.entities.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@CustomLog
public class CoverageUtils {

    public static CoverageEntity buildCoverageEntity(String xPagopaPnUid, CreateCoverageRequest request) {

        CoverageEntity coverageEntity = new CoverageEntity();

        coverageEntity.setUid(xPagopaPnUid);
        coverageEntity.setCreationTimestamp(Instant.now());
        coverageEntity.setCap(request.getCap());
        coverageEntity.setLocality(request.getLocality());
        coverageEntity.setProvince(request.getProvince());
        coverageEntity.setCadastralCode(request.getCadastralCode());

        return coverageEntity;

    }

}

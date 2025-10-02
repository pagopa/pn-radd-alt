package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.entities.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@CustomLog
public class CoverageUtils {

    public static CoverageEntity buildCoverageEntity(CreateCoverageRequest request) {

        CoverageEntity coverageEntity = new CoverageEntity();

        coverageEntity.setCap(request.getCap());
        coverageEntity.setLocality(request.getLocality());
        coverageEntity.setProvince(request.getProvince());
        coverageEntity.setCadastralCode(request.getCadastralCode());

        return coverageEntity;

    }

}

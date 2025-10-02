package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.entities.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@CustomLog
public class CoverageUtils {

    public static CoverageEntity mapFieldToUpdate(CoverageEntity coverageEntity, UpdateCoverageRequest request) {

        if (StringUtils.isNotBlank(request.getCadastralCode())) {
            coverageEntity.setCadastralCode(request.getCadastralCode());
        }

        if (StringUtils.isNotBlank(request.getProvince())) {
            coverageEntity.setProvince(request.getProvince());
        }

        if (request.getStartValidity() != null) {
            coverageEntity.setStartValidity(request.getStartValidity());
        }

        if (request.getEndValidity() != null) {
            coverageEntity.setEndValidity(request.getEndValidity());
        }

        return coverageEntity;

    }

}

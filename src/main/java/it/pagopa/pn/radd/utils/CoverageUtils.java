package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.entities.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.radd.utils.DateUtils.validateCoverageDateInterval;

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

        validateCoverageDateInterval(coverageEntity.getStartValidity(),coverageEntity.getEndValidity(), request.getStartValidity(), request.getEndValidity());

        if (request.getStartValidity() != null) {
            coverageEntity.setStartValidity(request.getStartValidity());
        }

        if (request.getEndValidity() != null) {
            coverageEntity.setEndValidity(request.getEndValidity());
        }

        return coverageEntity;

    }

}

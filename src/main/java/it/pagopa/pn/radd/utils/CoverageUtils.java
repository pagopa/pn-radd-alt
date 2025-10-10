package it.pagopa.pn.radd.utils;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.middleware.db.entities.*;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static it.pagopa.pn.radd.utils.DateUtils.validateCoverageDateInterval;

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


    public static CoverageEntity mapFieldToUpdate(String xPagopaPnUid, CoverageEntity coverageEntity, UpdateCoverageRequest request) {

        if (StringUtils.isNotBlank(xPagopaPnUid)) {
            coverageEntity.setUid(xPagopaPnUid);
        }

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

        coverageEntity.setUpdateTimestamp(Instant.now());

        return coverageEntity;

    }

}

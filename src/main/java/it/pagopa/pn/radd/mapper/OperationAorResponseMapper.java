package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.OperationAorDetailResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.OperationAorResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.OperationResponseStatus;
import it.pagopa.pn.radd.exception.ExceptionTypeEnum;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity;
import it.pagopa.pn.radd.utils.Const;
import it.pagopa.pn.radd.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class OperationAorResponseMapper {

    private OperationAorResponseMapper () {
        // do nothing
    }

    public static OperationAorResponse fromResult(RaddTransactionEntity result){
        OperationAorResponse response = new OperationAorResponse();
        response.setElement(getDetail(result));
        response.setResult(true);
        OperationResponseStatus status = new OperationResponseStatus();
        status.setCode(OperationResponseStatus.CodeEnum.NUMBER_0);
        status.setMessage(Const.OK);
        response.setStatus(status);
        return response;
    }

    public static OperationAorResponse fromException(RaddGenericException ex) {
        OperationAorResponse r = new OperationAorResponse();
        OperationResponseStatus status = new OperationResponseStatus();
        r.setResult(false);
        status.setMessage(ex.getExceptionType().getMessage());
        status.setCode((ex.getExceptionType() == ExceptionTypeEnum.TRANSACTION_NOT_EXIST) ?
                OperationResponseStatus.CodeEnum.NUMBER_1 : OperationResponseStatus.CodeEnum.NUMBER_99);
        r.setStatus(status);
        return r;
    }

    public static OperationAorDetailResponse getDetail(RaddTransactionEntity source){
        OperationAorDetailResponse dto = new OperationAorDetailResponse();
        dto.setOperationId(source.getOperationId());
        String array = source.getIun().substring(1, source.getIun().length()-1);
        List<String> iuns = List.of(StringUtils.split(array, ","));
        dto.setIuns(iuns);
        dto.setRecipientTaxId(source.getRecipientId());
        dto.setRecipientType(source.getRecipientType());
        dto.setDelegateTaxId(source.getDelegateId());
        dto.setUid(source.getUid());
        dto.setFileKey(source.getFileKey());
        dto.setOperationEndDate(DateUtils.parseDateString(source.getOperationEndDate()));
        dto.setOperationStartDate(DateUtils.parseDateString(source.getOperationStartDate()));
        dto.setOperationStatus(source.getStatus());
        dto.setErrorReason(source.getErrorReason());
        dto.setQrCode(source.getQrCode());
        dto.setOperationType(source.getOperationType());
        return dto;
    }
}

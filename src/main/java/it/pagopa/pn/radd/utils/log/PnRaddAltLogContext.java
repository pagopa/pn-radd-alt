package it.pagopa.pn.radd.utils.log;

import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndeliverypush.v1.dto.ResponsePaperNotificationFailedDtoDto;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.radd.utils.Utils;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import static it.pagopa.pn.commons.utils.LogUtils.maskTaxId;

public class PnRaddAltLogContext {
    private String uid = "";
    private String cxId = "";
    private String cxType = "";
    private String recipientInternalId = "";
    private String delegateInternalId = "";
    private String transactionId = "";
    private String requestFileKey = "";
    private String downloadedFilekeys = "";
    private String result = "";
    private String status = "";
    private String operationId = "";
    private String iun = "";
    private String iuns = "";
    private String aarFilekeys = "";
    private String taxCode = "";
    private String sourceChannel = "";

    public PnRaddAltLogContext addUid(String uid) {
        this.uid = "uid=%s ".formatted(uid);
        return this;
    }

    public PnRaddAltLogContext addCxId(String cxId) {
        this.cxId = "cxId=%s ".formatted(cxId);
        return this;
    }

    public PnRaddAltLogContext addCxType(String cxType) {
        this.cxType = "cxType=%s ".formatted(cxType);
        return this;
    }

    public PnRaddAltLogContext addRecipientInternalId(String recipientInternalId) {
        this.recipientInternalId = "recipientInternalId=%s ".formatted(recipientInternalId);
        return this;
    }

    public PnRaddAltLogContext addDelegateInternalId(String delegateInternalId) {
        this.delegateInternalId = "delegateInternalId=%s ".formatted(delegateInternalId);
        return this;
    }

    public PnRaddAltLogContext addTransactionId(String transactionId) {
        this.transactionId = "transactionId=%s ".formatted(transactionId);
        return this;
    }

    public PnRaddAltLogContext addRequestFileKey(String requestFileKey) {
        this.requestFileKey = "requestFileKey=%s ".formatted(requestFileKey);
        return this;
    }

    public PnRaddAltLogContext addDownloadFilekeys(List<DownloadUrl> downloadUrlList) {
        List<String> presignedUrls = downloadUrlList.stream().map(DownloadUrl::getUrl).toList();
        String joinedFileKeys = presignedUrls.stream().map(Utils::getFileKeyFromPresignedUrl).collect(Collectors.joining(", "));
        this.downloadedFilekeys = "downloadedFilekeys=[ %s ] ".formatted(joinedFileKeys);
        return this;
    }

    public PnRaddAltLogContext addAarFilekeys(List<ResponsePaperNotificationFailedDtoDto> responsePaperNotificationFailed) {
        List<String> presignedUrls = responsePaperNotificationFailed.stream().map(ResponsePaperNotificationFailedDtoDto::getAarUrl).toList();
        String joinedFileKeys = String.join(", ", presignedUrls);
        this.aarFilekeys = "aarFilekeys=[ %s ] ".formatted(joinedFileKeys);
        return this;
    }

    public PnRaddAltLogContext addResponseResult(Boolean result) {
        this.result = "result=%s ".formatted(result);
        return this;
    }

    public PnRaddAltLogContext addResponseStatus(String status) {
        this.status = "status=%s ".formatted(status);
        return this;
    }

    public PnRaddAltLogContext addOperationId(String operationId) {
        this.operationId = "operationId=%s ".formatted(operationId);
        return this;
    }

    public PnRaddAltLogContext addIun(String iun) {
        this.iun = "iun=%s ".formatted(iun);
        return this;
    }

    public PnRaddAltLogContext addIuns(List<ResponsePaperNotificationFailedDtoDto> responsePaperNotificationFailed) {
        List<String> iuns = responsePaperNotificationFailed.stream().map(ResponsePaperNotificationFailedDtoDto::getIun).toList();
        String joinedIuns = String.join(", ", iuns);
        this.iuns = "iuns=[ %s ] ".formatted(joinedIuns);
        return this;
    }

    public PnRaddAltLogContext addTaxCode(String taxCode) {
        this.taxCode = "taxCode=%s ".formatted(maskTaxId(taxCode));
        return this;
    }

    public PnRaddAltLogContext addSourceChannel(String sourceChannel) {
        if (StringUtils.hasText(sourceChannel)) {
            this.sourceChannel = "sourceChannel=%s ".formatted(sourceChannel);
        }
        return this;
    }

    public String logContext() {
        String context = uid + cxId + cxType + taxCode + operationId + transactionId + recipientInternalId + delegateInternalId + requestFileKey
                + iun + downloadedFilekeys + aarFilekeys + iuns + result + status + sourceChannel;
        return context.trim();
    }


}

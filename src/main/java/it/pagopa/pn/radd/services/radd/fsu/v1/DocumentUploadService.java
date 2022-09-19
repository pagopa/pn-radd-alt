package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.exception.PnInvalidInputException;
import it.pagopa.pn.radd.microservice.msclient.generated.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.radd.middleware.msclient.PnSafeStorageClient;
import it.pagopa.pn.radd.rest.radd.v1.dto.DocumentUploadRequest;
import it.pagopa.pn.radd.rest.radd.v1.dto.DocumentUploadResponse;
import it.pagopa.pn.radd.rest.radd.v1.dto.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DocumentUploadService {

    private final PnSafeStorageClient pnSafeStorageClient;


    public DocumentUploadService(PnSafeStorageClient pnSafeStorageClient) {
        this.pnSafeStorageClient = pnSafeStorageClient;
    }

    public Mono<DocumentUploadResponse> createFile(String uid, Mono<DocumentUploadRequest> documentUploadRequest) {
        // retrieve presigned url
        return documentUploadRequest
                .map(m -> {
                    if (m == null || StringUtils.isEmpty(m.getContentType()) || StringUtils.isEmpty(m.getBundleId())) {
                        log.error("Missing input parameters");
                        throw new PnInvalidInputException();
                    }
                    return m;
                })
                .flatMap(value -> {
                    return pnSafeStorageClient.createFile(value.getContentType(), value.getBundleId());
                })
                .map(item -> {
                    FileCreationResponseDto response = item;
                    log.info("Response presigned url : {}", response.getUploadUrl());
                    DocumentUploadResponse resp = new DocumentUploadResponse();
                    resp.setUrl(item.getUploadUrl()) ;
                    ResponseStatus status = new ResponseStatus();
                    status.code(ResponseStatus.CodeEnum.NUMBER_0);
                    resp.setStatus(status);
                    return resp;
                });
    }

}

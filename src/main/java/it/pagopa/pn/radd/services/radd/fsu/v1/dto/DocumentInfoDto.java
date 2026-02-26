package it.pagopa.pn.radd.services.radd.fsu.v1.dto;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.DownloadUrl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentInfoDto {
    private String fileKey;
    private Integer numberOfPages;
    private DownloadUrl downloadUrl;
    private String contentType;
}

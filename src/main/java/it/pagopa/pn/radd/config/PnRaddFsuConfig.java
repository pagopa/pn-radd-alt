package it.pagopa.pn.radd.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.radd.pojo.DocumentTypeEnum;
import it.pagopa.pn.radd.utils.HtmlSanitizer;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "pn.radd")
@Import(SharedAutoConfiguration.class)
public class PnRaddFsuConfig {
    private Integer attemptBatchWriter;
    private String clientDeliveryBasepath;
    private String clientDeliveryPushBasepath;
    private String clientDeliveryPushInternalBasepath;
    private String clientDatavaultBasepath;
    private String clientSafeStorageBasepath;
    private String safeStorageCxId;
    private String safeStorageDocType;
    private String applicationBasepath;
    private Dao dao;

    private HtmlSanitizer.SanitizeMode sanitizeMode;
    private List<DocumentTypeEnum> documentTypeEnumFilter = new ArrayList<>();

    @Data
    private static class Dao {
        private String raddTransactionTable;
        private String iunsOperationsTable;
        private String raddRegistryRequestTable;
    }
}

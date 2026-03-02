package it.pagopa.pn.radd.pojo;

import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndelivery.v1.dto.NotificationAttachmentDownloadMetadataResponseDto;
import lombok.Data;

@Data
public class NotificationAttachment {
    public NotificationAttachment(AttachmentType attachmentType, NotificationAttachmentDownloadMetadataResponseDto notificationAttachmentDownloadMetadataResponseDto, String fileKey) {
        this.type = attachmentType;
        this.notificationMetadata = notificationAttachmentDownloadMetadataResponseDto;
        this.fileKey = fileKey;
    }

    public enum AttachmentType {
        PAGOPA,
        F24,
        DOCUMENT
    }

    private AttachmentType type;

    private NotificationAttachmentDownloadMetadataResponseDto notificationMetadata;

    private String fileKey;

    public AttachmentType getType() {
        return type;
    }

    public void setType(AttachmentType type) {
        this.type = type;
    }
}

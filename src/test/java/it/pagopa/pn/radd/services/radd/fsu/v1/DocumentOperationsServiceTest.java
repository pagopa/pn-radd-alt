package it.pagopa.pn.radd.services.radd.fsu.v1;

import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndelivery.v1.dto.NotificationRecipientV24Dto;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pndelivery.v1.dto.SentNotificationV25Dto;
import it.pagopa.pn.radd.alt.generated.openapi.msclient.pnsafestorage.v1.dto.FileCreationResponseDto;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.DocumentUploadRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.DocumentUploadResponse;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.ResponseStatus;
import it.pagopa.pn.radd.config.PnRaddFsuConfig;
import it.pagopa.pn.radd.exception.PnInvalidInputException;
import it.pagopa.pn.radd.exception.PnRaddForbiddenException;
import it.pagopa.pn.radd.exception.RaddGenericException;
import it.pagopa.pn.radd.exception.TransactionAlreadyExistsException;
import it.pagopa.pn.radd.middleware.db.OperationsIunsDAO;
import it.pagopa.pn.radd.middleware.db.entities.OperationsIunsEntity;
import it.pagopa.pn.radd.middleware.db.entities.RaddTransactionEntity;
import it.pagopa.pn.radd.middleware.db.impl.RaddTransactionDAOImpl;
import it.pagopa.pn.radd.middleware.msclient.DocumentDownloadClient;
import it.pagopa.pn.radd.middleware.msclient.PnDeliveryClient;
import it.pagopa.pn.radd.middleware.msclient.PnSafeStorageClient;
import it.pagopa.pn.radd.utils.Const;
import it.pagopa.pn.radd.utils.PdfGenerator;
import it.pagopa.pn.radd.utils.RaddRole;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.*;

import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.DOCUMENT_UPLOAD_ERROR;
import static it.pagopa.pn.radd.exception.ExceptionTypeEnum.TRANSACTION_NOT_EXIST;
import static it.pagopa.pn.radd.utils.ZipUtils.extractPdfFromZip;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class DocumentOperationsServiceTest {
    @InjectMocks
    DocumentOperationsService documentOperationsService;

    @Mock
    PnDeliveryClient pnDeliveryClient;

    @Mock
    PnSafeStorageClient pnSafeStorageClient;

    @Mock
    RaddTransactionDAOImpl raddTransactionDAOImpl;

    @Mock
    PdfGenerator pdfGenerator;

    @Mock
    DocumentDownloadClient documentDownloadClient;

    @Mock
    OperationsIunsDAO operationsIunsDAO;

    @Mock
    PnRaddFsuConfig pnRaddFsuConfig;

    enum FailureStep {
        GET_TRANSACTION,
        ADD_SENDER_PA_ID
    }

    @Test
    void documentDownloadACTTest() throws IOException {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));
        sentNotificationV21Dto.setSenderPaId("senderPaId");

        byte[] response = new byte[0];
        byte[] responseHex = HexFormat.of().parseHex(Hex.encodeHexString(response));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(raddTransactionDAOImpl.addSenderPaId(any(), any(), any())).thenReturn(Mono.empty());
        when(pnDeliveryClient.getNotifications(any())).thenReturn(Mono.just(sentNotificationV21Dto));
        when(pdfGenerator.generateCoverFile(any(), any())).thenReturn(response);

        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectNext(responseHex)
                .verifyComplete();

    }

    @ParameterizedTest(name = "{index}: Fail on {0}")
    @EnumSource(FailureStep.class)
    void documentDownloadACT_NonExistingTransaction_Test(FailureStep failOn) {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));
        sentNotificationV21Dto.setSenderPaId("senderPaId");

        RaddGenericException nonExistingTransactionError = new RaddGenericException(TRANSACTION_NOT_EXIST, HttpStatus.BAD_REQUEST);
        if (failOn.equals(FailureStep.GET_TRANSACTION)) {
            when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.error(nonExistingTransactionError));
        } else if (failOn.equals(FailureStep.ADD_SENDER_PA_ID)) {
            when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
            when(raddTransactionDAOImpl.addSenderPaId(any(), any(), any())).thenReturn(Mono.error(nonExistingTransactionError));
            when(pnDeliveryClient.getNotifications(any())).thenReturn(Mono.just(sentNotificationV21Dto));
        } else {
            fail("Invalid failOn value");
        }

        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectErrorMatches(throwable -> throwable instanceof RaddGenericException e &&
                        e.getExceptionType() == TRANSACTION_NOT_EXIST)
                .verify();
    }

    @Test
    void documentDownloadWithAttachmentIdTest() {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setZipAttachments(Map.of("123", "123"));

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));

        byte[] zipFile = getFile();
        byte[] pdfFile = extractPdfFromZip(zipFile);
        byte[] responseHex = HexFormat.of().parseHex(Hex.encodeHexString(pdfFile));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(documentDownloadClient.downloadContent(any())).thenReturn(Mono.just(zipFile));

        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", "123"))
                .expectNextMatches(res -> Arrays.equals(res, responseHex))
                .verifyComplete();
    }

    @Test
    void documentDownloadAORTest() throws IOException {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));

        OperationsIunsEntity operationsIunsEntity = new OperationsIunsEntity();
        operationsIunsEntity.setIun("123");
        operationsIunsEntity.setTransactionId("123");

        byte[] response = new byte[0];
        byte[] responseHex = HexFormat.of().parseHex(Hex.encodeHexString(response));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(raddTransactionDAOImpl.addSenderPaId(any(), any(), any())).thenReturn(Mono.empty());
        when(pnDeliveryClient.getNotifications(any())).thenReturn(Mono.just(sentNotificationV21Dto));
        when(pdfGenerator.generateCoverFile(any(), any())).thenReturn(response);
        when(operationsIunsDAO.getAllIunsFromTransactionId(any())).thenReturn(Flux.just(operationsIunsEntity));

        StepVerifier.create(documentOperationsService.documentDownload("AOR", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectNext(responseHex)
                .verifyComplete();

    }

    @Test
    void documentDownloadAORMoreNotificationsTest() throws IOException {
        String recipientId = "recipientId";

        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId(recipientId);
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");

        List<SentNotificationV25Dto> notifications = createNotifications(recipientId, 2);
        List<OperationsIunsEntity> operationsIunsEntities = createOperationsIunsEntities(2);

        byte[] response = new byte[0];
        byte[] responseHex = HexFormat.of().parseHex(Hex.encodeHexString(response));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(pdfGenerator.generateCoverFile(any(), any())).thenReturn(response);
        for (int i = 0; i < notifications.size(); i++) {
            when(pnDeliveryClient.getNotifications(operationsIunsEntities.get(i).getIun())).thenReturn(Mono.just(notifications.get(i)));
            when(raddTransactionDAOImpl.addSenderPaId(any(), any(), eq(notifications.get(i).getSenderPaId()))).thenReturn(Mono.empty());
        }
        when(operationsIunsDAO.getAllIunsFromTransactionId(any())).thenReturn(Flux.fromIterable(operationsIunsEntities));

        StepVerifier.create(documentOperationsService.documentDownload("AOR", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectNext(responseHex)
                .verifyComplete();

        // verify that multi-notification enrichment logic is executed once per IUN/notification
        for (int i = 0; i < operationsIunsEntities.size(); i++) {
            verify(pnDeliveryClient, times(1)).getNotifications(operationsIunsEntities.get(i).getIun());
            verify(raddTransactionDAOImpl, times(1)).addSenderPaId(any(), any(), eq(notifications.get(i).getSenderPaId()));
        }

    }

    private List<SentNotificationV25Dto> createNotifications(String recipientId, int max) {
        List<SentNotificationV25Dto> notifications = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            SentNotificationV25Dto notification = new SentNotificationV25Dto();
            NotificationRecipientV24Dto recipient = new NotificationRecipientV24Dto();
            recipient.setInternalId(recipientId);
            recipient.setDenomination("denomination" + i);
            notification.setRecipients(List.of(recipient));
            notification.setSenderPaId("senderPaId" + i);
            notifications.add(notification);
        }
        return notifications;
    }

    private List<OperationsIunsEntity> createOperationsIunsEntities(int max) {
        List<OperationsIunsEntity> operationsIunsEntities = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            OperationsIunsEntity operationsIunsEntity = new OperationsIunsEntity();
            operationsIunsEntity.setIun("IUN_" + i);
            operationsIunsEntity.setTransactionId("transactionId" + i);
            operationsIunsEntities.add(operationsIunsEntity);
        }
        return operationsIunsEntities;
    }

    @Test
    void calculateTotalPages_withValidAttachments_returnsSumPlusOne() {
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setDocAttachments(Map.of("key1", 3, "key2", 5, "key3", 1));
        entity.setZipAttachments(Map.of("zipKey", "https://some-url"));

        Integer result = documentOperationsService.calculateTotalPages(entity);

        assertEquals(11, result); // 3 + 5 + 1 + 1 (frontespizio) + 1 (file zip)
    }

    @Test
    void calculateTotalPages_withValidAttachmentsAndNoZip_returnsSumPlusFrontespizioOnly() {
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setDocAttachments(Map.of("key1", 3, "key2", 5, "key3", 1));
        // no zipAttachments set

        Integer result = documentOperationsService.calculateTotalPages(entity);

        assertEquals(10, result); // 3 + 5 + 1 + 1 (frontespizio only, no zip)
    }

    @Test
    void calculateTotalPages_withNullDocAttachments_returnsNull() {
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setDocAttachments(null);

        assertNull(documentOperationsService.calculateTotalPages(entity));
    }

    @Test
    void calculateTotalPages_withEmptyDocAttachments_returnsNull() {
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setDocAttachments(Collections.emptyMap());

        assertNull(documentOperationsService.calculateTotalPages(entity));
    }

    @Test
    void calculateTotalPages_withSomeNullPages_returnsNull() {
        Map<String, Integer> attachments = new HashMap<>();
        attachments.put("key1", 3);
        attachments.put("key2", null);
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setDocAttachments(attachments);

        assertNull(documentOperationsService.calculateTotalPages(entity));
    }

    @Test
    void calculateTotalPages_withZeroPages_returnsNull() {
        Map<String, Integer> attachments = new HashMap<>();
        attachments.put("key1", 3);
        attachments.put("key2", 0);
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setDocAttachments(attachments);

        assertNull(documentOperationsService.calculateTotalPages(entity));
    }

    @Test
    void calculateTotalPages_withNegativePages_returnsNull() {
        Map<String, Integer> attachments = new HashMap<>();
        attachments.put("key1", 3);
        attachments.put("key2", -1);
        RaddTransactionEntity entity = new RaddTransactionEntity();
        entity.setDocAttachments(attachments);

        assertNull(documentOperationsService.calculateTotalPages(entity));
    }

    @Test
    void documentDownloadACT_withDocAttachments_generatesCoverWithPageCount() throws IOException {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");

        Map<String, Integer> docAttachments = new HashMap<>();
        docAttachments.put("key1", 3);
        docAttachments.put("key2", 5);
        raddTransactionEntity.setDocAttachments(docAttachments);
        raddTransactionEntity.setZipAttachments(Map.of("zipKey", "https://some-url"));

        SentNotificationV25Dto sentNotificationV25Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto recipient = new NotificationRecipientV24Dto();
        recipient.setInternalId("123");
        recipient.setDenomination("denomination");
        sentNotificationV25Dto.setRecipients(List.of(recipient));
        sentNotificationV25Dto.setSenderPaId("senderPaId");

        byte[] response = new byte[0];
        byte[] responseHex = HexFormat.of().parseHex(Hex.encodeHexString(response));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(raddTransactionDAOImpl.addSenderPaId(any(), any(), any())).thenReturn(Mono.empty());
        when(pnDeliveryClient.getNotifications(any())).thenReturn(Mono.just(sentNotificationV25Dto));
        when(pdfGenerator.generateCoverFile(any(), any())).thenReturn(response);

        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectNext(responseHex)
                .verifyComplete();

        verify(pdfGenerator).generateCoverFile("denomination", 10);
    }

    @Test
    void documentDownloadACT_withNullDocAttachments_generatesCoverWithoutPageCount() throws IOException {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");
        raddTransactionEntity.setDocAttachments(null);

        SentNotificationV25Dto sentNotificationV25Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto recipient = new NotificationRecipientV24Dto();
        recipient.setInternalId("123");
        recipient.setDenomination("denomination");
        sentNotificationV25Dto.setRecipients(List.of(recipient));
        sentNotificationV25Dto.setSenderPaId("senderPaId");

        byte[] response = new byte[0];
        byte[] responseHex = HexFormat.of().parseHex(Hex.encodeHexString(response));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(raddTransactionDAOImpl.addSenderPaId(any(), any(), any())).thenReturn(Mono.empty());
        when(pnDeliveryClient.getNotifications(any())).thenReturn(Mono.just(sentNotificationV25Dto));
        when(pdfGenerator.generateCoverFile(any(), any())).thenReturn(response);

        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectNext(responseHex)
                .verifyComplete();

        verify(pdfGenerator).generateCoverFile("denomination", null);
    }

    @Test
    void documentDownloadACT_withPartiallyNullPages_generatesCoverWithoutPageCount() throws IOException {
        Map<String, Integer> partialAttachments = new HashMap<>();
        partialAttachments.put("key1", 3);
        partialAttachments.put("key2", null);

        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");
        raddTransactionEntity.setDocAttachments(partialAttachments);

        SentNotificationV25Dto sentNotificationV25Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto recipient = new NotificationRecipientV24Dto();
        recipient.setInternalId("123");
        recipient.setDenomination("denomination");
        sentNotificationV25Dto.setRecipients(List.of(recipient));
        sentNotificationV25Dto.setSenderPaId("senderPaId");

        byte[] response = new byte[0];
        byte[] responseHex = HexFormat.of().parseHex(Hex.encodeHexString(response));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(raddTransactionDAOImpl.addSenderPaId(any(), any(), any())).thenReturn(Mono.empty());
        when(pnDeliveryClient.getNotifications(any())).thenReturn(Mono.just(sentNotificationV25Dto));
        when(pdfGenerator.generateCoverFile(any(), any())).thenReturn(response);

        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectNext(responseHex)
                .verifyComplete();

        verify(pdfGenerator).generateCoverFile("denomination", null);
    }

    @Test
    void documentDownloadAORNoRecipientTest() {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);
        raddTransactionEntity.setIun("123");

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));

        OperationsIunsEntity operationsIunsEntity = new OperationsIunsEntity();
        operationsIunsEntity.setIun("123");
        operationsIunsEntity.setTransactionId("123");

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        when(raddTransactionDAOImpl.addSenderPaId(any(), any(), any())).thenReturn(Mono.empty());
        when(pnDeliveryClient.getNotifications(any())).thenReturn(Mono.just(sentNotificationV21Dto));
        when(operationsIunsDAO.getAllIunsFromTransactionId(any())).thenReturn(Flux.just(operationsIunsEntity));

        StepVerifier.create(documentOperationsService.documentDownload("AOR", "ACT", CxTypeAuthFleet.PF, "cxId", null))
                .expectError(RaddGenericException.class)
                .verify();

    }


    private byte[] getFile() {
        try {
            return new ClassPathResource("zip/zip-with-pdf.zip").getInputStream().readAllBytes();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void documentDownloadValidateErrorTest() {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));

        StepVerifier.create(documentOperationsService.documentDownload("", "ACT", CxTypeAuthFleet.PF, "cxId", "123"))
                .expectError(PnInvalidInputException.class)
                .verify();

    }

    @Test
    void documentDownloadValidateErrorTest2() {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));

        StepVerifier.create(documentOperationsService.documentDownload("ACT", "", CxTypeAuthFleet.PF, "cxId", "123"))
                .expectError(PnInvalidInputException.class)
                .verify();

    }

    @Test
    void documentDownloadAbortedStatusErrorTest() {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.ABORTED);

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", "123"))
                .expectError(TransactionAlreadyExistsException.class)
                .verify();

    }

    @Test
    void documentDownloadPdfGeneratorErrorTest() {
        RaddTransactionEntity raddTransactionEntity = new RaddTransactionEntity();
        raddTransactionEntity.setRecipientId("123");
        raddTransactionEntity.setStatus(Const.STARTED);

        SentNotificationV25Dto sentNotificationV21Dto = new SentNotificationV25Dto();
        NotificationRecipientV24Dto notificationRecipientV21Dto = new NotificationRecipientV24Dto();
        notificationRecipientV21Dto.setInternalId("123");
        notificationRecipientV21Dto.setDenomination("denomination");
        sentNotificationV21Dto.setRecipients(List.of(notificationRecipientV21Dto));

        when(raddTransactionDAOImpl.getTransaction(any(), any())).thenReturn(Mono.just(raddTransactionEntity));
        StepVerifier.create(documentOperationsService.documentDownload("ACT", "ACT", CxTypeAuthFleet.PF, "cxId", "123"))
                .expectError(RaddGenericException.class)
                .verify();

    }


    @Test
    void testWhenIdAndBoundleKO(){
        DocumentUploadRequest bundleId = new DocumentUploadRequest() ;
        Mockito.when(pnSafeStorageClient.createFile( any(), any())
        ).thenReturn(Mono.error( new RaddGenericException(DOCUMENT_UPLOAD_ERROR)));
        when(pnRaddFsuConfig.getSafeStorageDocType()).thenReturn("test");
        Mono<DocumentUploadResponse> response = documentOperationsService.createFile(Mono.just(bundleId),String.valueOf(RaddRole.RADD_UPLOADER));
        response.onErrorResume(ex ->{
            if (ex instanceof RaddGenericException){
                log.info(((RaddGenericException) ex).getExceptionType().getMessage());
                assertNotNull(ex);
                assertNotNull(((RaddGenericException) ex).getExceptionType());
                assertEquals( DOCUMENT_UPLOAD_ERROR  , ((RaddGenericException) ex).getExceptionType());
                return Mono.empty();
            }
            fail("Other exception");
            return null;
        }).block();
    }


    @Test
    void testWhenIdAndBoundleId(){
        DocumentUploadRequest bundleId = new DocumentUploadRequest() ;
        FileCreationResponseDto fileCreationResponseDto = mock(FileCreationResponseDto.class);
        fileCreationResponseDto.setUploadUrl("testUrl");
        when(pnRaddFsuConfig.getSafeStorageDocType()).thenReturn("test");
        Mockito.when(pnSafeStorageClient.createFile(Mockito.any(), Mockito.any())
        ).thenReturn( Mono.just(fileCreationResponseDto) );
        DocumentUploadResponse response = documentOperationsService.createFile(Mono.just(bundleId),String.valueOf(RaddRole.RADD_UPLOADER)).block();
        assertNotNull(response);
        assertEquals(ResponseStatus.CodeEnum.NUMBER_0, response.getStatus().getCode());
    }

    @Test
    void testWhenRequestIsNull(){

        Mono <DocumentUploadResponse> response = documentOperationsService.createFile(null, String.valueOf(RaddRole.RADD_UPLOADER));
        response.onErrorResume( PnInvalidInputException.class, exception ->{
            assertEquals("Body non valido", exception.getMessage());
            return Mono.empty();
        }).block();

    }

    @Test
    void testWhenRoleIsInvalidThenThrowsException(){
        DocumentUploadRequest bundleId = new DocumentUploadRequest() ;
        assertThrows(PnRaddForbiddenException.class, () -> documentOperationsService.createFile(Mono.just(bundleId), String.valueOf(RaddRole.RADD_STANDARD)));
    }
    @Test
    void testWhenRoleIsNullThenThrowsException(){
        DocumentUploadRequest bundleId = new DocumentUploadRequest() ;
        assertThrows(PnRaddForbiddenException.class, () -> documentOperationsService.createFile(Mono.just(bundleId), null));
    }

}


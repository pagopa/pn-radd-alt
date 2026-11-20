package it.pagopa.pn.radd.mapper;

import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.ActStartTransactionRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.AorStartTransactionRequest;
import it.pagopa.pn.radd.alt.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.radd.pojo.TransactionData;
import it.pagopa.pn.radd.utils.OperationTypeEnum;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionDataMapperTest {

    @InjectMocks
    private TransactionDataMapper mapper;

    @ParameterizedTest
    @EnumSource(CxTypeAuthFleet.class)
    void mapsActAuditIdentity(CxTypeAuthFleet cxType) {
        ActStartTransactionRequest request = new ActStartTransactionRequest();
        request.setOperationId("operation-id");
        request.setRecipientType(ActStartTransactionRequest.RecipientTypeEnum.PF);

        TransactionData transaction = mapper.toTransaction("audit-uid", request, cxType, "audit-cx-id");

        assertIdentity(transaction, cxType);
        assertEquals(OperationTypeEnum.ACT, transaction.getOperationType());
    }

    @ParameterizedTest
    @EnumSource(CxTypeAuthFleet.class)
    void mapsAorAuditIdentity(CxTypeAuthFleet cxType) {
        AorStartTransactionRequest request = new AorStartTransactionRequest();
        request.setOperationId("operation-id");
        request.setRecipientType(AorStartTransactionRequest.RecipientTypeEnum.PF);

        TransactionData transaction = mapper.toTransaction("audit-uid", request, cxType, "audit-cx-id");

        assertIdentity(transaction, cxType);
        assertEquals(OperationTypeEnum.AOR, transaction.getOperationType());
    }

    private static void assertIdentity(TransactionData transaction, CxTypeAuthFleet cxType) {
        assertEquals("audit-uid", transaction.getUid());
        assertEquals("audit-cx-id", transaction.getCxId());
        assertEquals(cxType.getValue(), transaction.getCxType());
        assertEquals(cxType.getValue() + "#audit-cx-id#operation-id", transaction.getTransactionId());
    }
}

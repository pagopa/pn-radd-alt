package it.pagopa.pn.radd.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionAlreadyExistsExceptionTest {
    /**
     * Method under test: default or parameterless constructor of {@link TransactionAlreadyExistsException}
     */
    @Test
    void testConstructor() {
        TransactionAlreadyExistsException actualTransactionAlreadyExistsException = new TransactionAlreadyExistsException();
        assertNull(actualTransactionAlreadyExistsException.getStatus());
        assertNotNull(actualTransactionAlreadyExistsException.getMessage());
        assertNull(actualTransactionAlreadyExistsException.getExtra());
        assertEquals(ExceptionTypeEnum.TRANSACTION_ALREADY_EXIST,
                actualTransactionAlreadyExistsException.getExceptionType());
    }
}


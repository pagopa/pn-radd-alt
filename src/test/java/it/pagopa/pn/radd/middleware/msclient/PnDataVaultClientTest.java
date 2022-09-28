package it.pagopa.pn.radd.middleware.msclient;

import it.pagopa.pn.radd.config.BaseTest;
import it.pagopa.pn.radd.exception.PnEnsureFiscalCodeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PnDataVaultClientTest extends BaseTest {

    @Autowired
    private PnDataVaultClient pnDataVaultClient;

    @Test
    void testGetEnsureFiscalCode() {
        String fiscalCode = "" , type = "PF";
        String responseFiscal = pnDataVaultClient.getEnsureFiscalCode(fiscalCode, type).block();
        assertEquals(responseFiscal, "\"PF-4fc75df3-0913-407e-bdaa-e50329708b7d\"");
    }

    @Test
    void testGetEnsureFiscalCodeError400() {
        String fiscalCode = "" , type = "PG";
        Mono<String> response = pnDataVaultClient.getEnsureFiscalCode(fiscalCode, type);
        response.onErrorResume(PnEnsureFiscalCodeException.class,  exception -> {
            assertEquals(400, exception.getWebClientEx().getStatusCode().value());
            return Mono.empty();
        }).block();
    }
}